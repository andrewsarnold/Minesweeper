package minesweeper;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainWindowController implements Initializable
{
	private Stage _stage;
	private Minefield _minefield;
	private Random _random;
	private int _width;
	private int _height;
	private Button[][] _buttons;
	private Level _difficultyLevel;
	private boolean _playing;
	private int _minesLeft;
	private Timer _timer;
	private int _secondsElapsed;
	
	@FXML MenuItem mnuNew;
	@FXML Menu mnuDifficulty;
	@FXML ImageView face;
	@FXML Label lblTimer;
	@FXML Label minesLeft;
	@FXML VBox mainArea;
	
	@Override
	public void initialize(URL url, ResourceBundle rb)
	{
		_difficultyLevel = Level.Beginner;
		_random = new Random();
		_timer = new Timer();
		_timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				Platform.runLater(() ->
				{
					if (_playing) SetTimerLabel();
				});
			}
		}, 1000, 1000);
		LoadLevels();
		StartANewGame();
	}
	
	public void SetPrimaryStage(Stage stage)
	{
		_stage = stage;
		_stage.setTitle("Minesweeper");
	}
	
	private void LoadLevels()
	{
		for (Level l : Level.values())
		{
			CheckMenuItem m = new CheckMenuItem(l.name());
			m.setOnAction(event ->
			{
				_difficultyLevel = l;
				mnuDifficulty.getItems().stream().forEach(mi -> ((CheckMenuItem)mi).setSelected(false));
				m.setSelected(true);
				StartANewGame();
			});
			mnuDifficulty.getItems().add(m);
		}
	}
	
	@FXML
	private void StartANewGame()
	{
		mainArea.getChildren().clear();
		_minefield = new Minefield(_difficultyLevel, _random);
		lblTimer.setText("0:00");
		_minesLeft = _minefield.MineCount();
		minesLeft.setText(Integer.toString(_minesLeft));
		_width = _minefield.Width();
		_height = _minefield.Height();
		_buttons = new Button[_width][_height];
		
		for (int y = 0; y < _minefield.Height(); y++)
		{
			HBox container = new HBox();
			for (int x = 0; x < _minefield.Width(); x++)
			{
				Button b = NewButton(x, y);
				_buttons[x][y] = b;
				container.getChildren().add(b);
			}
			mainArea.getChildren().add(container);
		}
		
		_stage.sizeToScene();
		SetFace("images/happy.png");
		
		_secondsElapsed = 0;
		_playing = true;
	}
	
	@FXML
	private void Quit()
	{
		_timer.cancel();
		_stage.close();
	}
	
	private void SetTimerLabel()
	{
		_secondsElapsed++;
		
		int minutes = _secondsElapsed / 60;
		int seconds = _secondsElapsed % 60;

		lblTimer.setText(String.format("%d:%02d", minutes, seconds));
	}
	
	private void Click(int x, int y)
	{
		Click(null, _buttons[x][y], x, y, true);
	}
	
	private void Click(MouseEvent e, Button b, int x, int y, boolean recursed)
	{
		if (e != null && e.getButton() == MouseButton.SECONDARY)
		{
			RightClick(b);
		}
		else
		{
			LeftClick(b, x, y);
			if (!recursed)
			{
				CheckIfWon();
			}
		}
	}
	
	private void RightClick(Button b)
	{
		if (!WasClicked(b))
		{
			ToggleFlag(b);
		}
	}
	
	private void LeftClick(Button b, int x, int y)
	{
		if (b.getGraphic() != null)
		{
			ClearImage(b);
			_minesLeft++;
			minesLeft.setText(Integer.toString(_minesLeft));
		}
		
		// If already clicked, return
		if (WasClicked(b)) return;
		
		int value = _minefield.OnClick(x, y);
		b.setOnAction(event -> {});
		b.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		
		switch (value)
		{
			case -1: // mine
				SetMine(b);
				GameOver("images/mine.png", "You stepped on a mine and exploded!", "Try again?", "images/dead.png");
				break;
			case 0: // nothing
				SetZero(b, x, y);
				break;
			default: // else
				SetText(b, value);
				break;
		}
	}
	
	private Button NewButton(int x, int y)
	{
		final int size = 30;
		Button b = new Button();
		b.setMinSize(size, size);
		b.setMaxSize(size, size);
		b.setOnMouseClicked(event -> Click(event, b, x, y, false));
		b.setOnMousePressed(event -> SetFace("images/surprised.png"));
		b.setOnMouseReleased(event -> SetFace("images/happy.png"));
		b.setStyle("-fx-font-weight: bold;");
		return b;
	}
	
	private void SetMine(Button b)
	{
		SetImage(b, "images/mine.png");
	}
	
	private void ToggleFlag(Button b)
	{
		if (b.getGraphic() == null)
		{
			_minesLeft--;
			SetImage(b, "images/flag.png");
		}
		else
		{
			_minesLeft++;
			ClearImage(b);
		}
		
		minesLeft.setText(Integer.toString(_minesLeft));
	}
	
	private void SetZero(Button b, int x, int y)
	{
		// Also click all adjacent tiles
		
		// Top left
		if (x > 0 && y > 0) Click(x - 1, y - 1);
		// Top center
		if (y > 0) Click(x, y - 1);
		// Top right
		if (x < _width - 1 && y > 0) Click(x + 1, y - 1);
		
		// Middle left
		if (x > 0) Click(x - 1, y);
		// Middle right
		if (x < _width - 1) Click(x + 1, y);
		
		// Bottom left
		if (x > 0 && y < _height - 1) Click(x - 1, y + 1);
		// Bottom center
		if (y < _height - 1) Click(x, y + 1);
		// Bottom right
		if (x < _width - 1 && y < _height - 1) Click(x + 1, y + 1);
	}
	
	private void SetText(Button b, int value)
	{
		ClearImage(b);
		b.setText(Integer.toString(value));
		
		Color color = Color.BLACK;
		switch (value)
		{
			case 1:
				color = Color.BLUE;
				break;
			case 2:
				color = Color.GREEN;
				break;
			case 3:
				color = Color.RED;
				break;
			case 4:
				color = Color.DARKBLUE;
				break;
			case 5:
				color = Color.DARKRED;
				break;
			case 6:
				color = Color.TEAL;
				break;
			case 7:
				color = Color.PURPLE;
				break;
		}
		
		b.setTextFill(color);
	}
	
	private void SetImage(Button b, String imageName)
	{
		b.setGraphic(GetImage(imageName, 20));
	}
	
	private Canvas GetImage(String imageName, int size)
	{
		InputStream is = MainWindowController.class.getResourceAsStream(imageName);
		Image i = new Image(is, size, size, false, true);
		Canvas c = new Canvas(size, size);
		GraphicsContext gc = c.getGraphicsContext2D();
		gc.drawImage(i, 0, 0);
		return c;
	}
	
	private void ClearImage(Button b)
	{
		b.setGraphic(null);
	}
	
	private void CheckIfWon()
	{
		// You win the game if all cells that are NOT mines have been clicked.
		// If there are (total - mineCount) clicked cells, you have won.
		
		int clickedCount = 0;
		for (int x = 0; x < _width; x++)
		{
			for (int y = 0; y < _height; y++)
			{
				if (WasClicked(_buttons[x][y]))
				{
					clickedCount++;
				}
			}
		}
		
		final int totalCells = _width * _height;
		if (clickedCount >= totalCells - _minefield.MineCount())
		{
			// You win!
			GameOver("images/flag.png", "You cleared the area of mines!", "Congratulations!", "images/cool.png");
		}
	}
	
	private boolean WasClicked(Button b)
	{
		Background back = b.getBackground();
		if (back == null) return false;
		List<BackgroundFill> fills = back.getFills();
		return !fills.isEmpty() && fills.get(0).getFill().equals(Color.WHITE);
	}
	
	private void GameOver(String imageName, String header, String body, String face)
	{
		SetFace(face);
		ShowAllMines();
		_playing = false;
		
		Alert alert = new Alert(AlertType.NONE);
		((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("minesweeper/images/mine.png"));
		alert.setGraphic(GetImage(imageName, 64));
		alert.getButtonTypes().clear();
		alert.getButtonTypes().addAll(
			new ButtonType("Start new game", ButtonData.YES),
			new ButtonType("Quit", ButtonData.CANCEL_CLOSE)
		);
		alert.setTitle("Minesweeper");
		alert.setHeaderText(header);
		alert.setContentText(body);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get().getButtonData() == ButtonData.YES)
		{
			StartANewGame();
		}
		else
		{
			Quit();
		}
	}
	
	private void ShowAllMines()
	{
		for (int y = 0; y < _height; y++)
		{
			for (int x = 0; x < _width; x++)
			{
				Button b = _buttons[x][y];
				if (_minefield.IsMine(x, y))
				{
					SetMine(b);
				}
			}
		}
	}
	
	private void SetFace(String image)
	{
		InputStream is = MainWindowController.class.getResourceAsStream(image);
		Image i = new Image(is);
		face.setImage(i);
	}
}
