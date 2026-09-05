import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pocasie {
	private static final Color INK = new Color(235, 241, 247);
	private static final Color MUTED = new Color(163, 181, 194);
	private static final Color PANEL = new Color(24, 32, 43, 245);
	private static final Color CARD = new Color(30, 41, 54, 230);
	private static final Color ACCENT = new Color(255, 190, 92);
	private static final Color FIELD_BACKGROUND = new Color(18, 27, 38);
	private static final Color FIELD_BORDER = new Color(74, 91, 108);
	private static final Color POPUP_BACKGROUND = new Color(25, 35, 47);
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Font UI = new Font("Segoe UI", Font.PLAIN, 14);
	private static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 14);

	public static void main(String[] args) {
		SwingUtilities.invokeLater(Pocasie::createWindow);
	}

	private static void createWindow() {
		JFrame frame = new JFrame("Počasie");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(1150, 760));
		frame.setSize(1280, 900);
		frame.setLocationRelativeTo(null);

		GradientPanel background = new GradientPanel();
		background.setLayout(new BorderLayout(0, 22));
		background.setBorder(new EmptyBorder(28, 34, 30, 34));

		JPanel header = new JPanel(new BorderLayout(18, 0));
		header.setOpaque(false);
		JLabel title = label("Dobré ráno, dnes bude príjemne", 24, INK, true);
		title.setBorder(new EmptyBorder(0, 0, 0, 18));
		header.add(title, BorderLayout.CENTER);

		JPanel search = new JPanel(new BorderLayout(8, 0));
		search.setOpaque(false);
		search.setPreferredSize(new Dimension(430, 48));
		JTextField cityField = new PlaceholderField("Napíš mesto alebo štát...");
		cityField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		cityField.setForeground(INK);
		cityField.setCaretColor(ACCENT);
		cityField.setEditable(true);
		cityField.setFocusable(true);
		cityField.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent event) {
				cityField.selectAll();
			}
		});
		cityField.setOpaque(true);
		cityField.setBackground(FIELD_BACKGROUND);
		cityField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(FIELD_BORDER, 1),
				new EmptyBorder(11, 15, 11, 15)));
		JButton searchButton = new RoundedButton("Hľadať");
		styleButton(searchButton, ACCENT, new Color(36, 43, 47));
		JToggleButton unitToggle = new RoundedToggleButton("°C");
		styleToggle(unitToggle);
		JButton favoritesButton = new RoundedButton("Obľúbené (0)");
		styleButton(favoritesButton, new Color(47, 62, 78), INK);
		favoritesButton.setToolTipText("Zobraziť obľúbené mestá");
		favoritesButton.setPreferredSize(new Dimension(155, 48));
		search.add(cityField, BorderLayout.CENTER);
		search.add(searchButton, BorderLayout.EAST);
		search.add(unitToggle, BorderLayout.WEST);
		JPanel headerActions = new JPanel(new BorderLayout(8, 0));
		headerActions.setOpaque(false);
		headerActions.add(search, BorderLayout.CENTER);
		headerActions.add(favoritesButton, BorderLayout.EAST);
		header.add(headerActions, BorderLayout.EAST);
		background.add(header, BorderLayout.NORTH);
		JPopupMenu suggestions = new JPopupMenu();
		suggestions.setBackground(POPUP_BACKGROUND);
		suggestions.setFocusable(false);
		suggestions.setLightWeightPopupEnabled(true);
		suggestions.setBorder(BorderFactory.createLineBorder(FIELD_BORDER, 1));
		final int[] suggestionRequest = {0};
		final Place[] selectedPlace = {null};
		List<Place> favoritePlaces = new ArrayList<>();
		JPopupMenu favoritesMenu = new JPopupMenu();
		favoritesMenu.setBackground(POPUP_BACKGROUND);
		favoritesButton.addActionListener(event -> {
			favoritesMenu.removeAll();
			JPanel favoritesPanel = new JPanel();
			favoritesPanel.setLayout(new javax.swing.BoxLayout(favoritesPanel, javax.swing.BoxLayout.Y_AXIS));
			favoritesPanel.setBackground(POPUP_BACKGROUND);
			favoritesPanel.setBorder(new EmptyBorder(14, 14, 14, 14));
			favoritesPanel.setPreferredSize(new Dimension(290, Math.max(82, 58 + favoritePlaces.size() * 44)));
			JLabel favoritesTitle = label("OBĽÚBENÉ MESTÁ", 11, MUTED, true);
			favoritesPanel.add(favoritesTitle);
			favoritesPanel.add(javax.swing.Box.createVerticalStrut(8));
			if (favoritePlaces.isEmpty()) {
				JLabel emptyLabel = label("Zatiaľ nemáš uložené žiadne mesto.", 13, INK, false);
				favoritesPanel.add(emptyLabel);
			} else {
				for (Place favoritePlace : favoritePlaces) {
					JButton item = new JButton(favoritePlace.description());
					item.setFont(UI_BOLD);
					item.setForeground(INK);
					item.setBackground(new Color(36, 50, 65));
					item.setHorizontalAlignment(JButton.LEFT);
					item.setFocusPainted(false);
					item.setBorder(new EmptyBorder(9, 12, 9, 12));
					item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
					item.addActionListener(favoriteEvent -> {
						cityField.setText(favoritePlace.name());
						selectedPlace[0] = favoritePlace;
						searchButton.doClick();
						favoritesMenu.setVisible(false);
					});
					favoritesPanel.add(item);
					favoritesPanel.add(javax.swing.Box.createVerticalStrut(5));
				}
			}
			favoritesMenu.add(favoritesPanel);
			favoritesMenu.show(favoritesButton, 0, favoritesButton.getHeight());
		});
		Timer suggestionDelay = new Timer(300, null);
		suggestionDelay.setRepeats(false);

		JPanel content = new JPanel(new BorderLayout(0, 18));
		content.setOpaque(false);
		WeatherView weatherView = new WeatherView();
		content.add(weatherView.currentWeather(), BorderLayout.NORTH);
		content.add(weatherView.forecast(), BorderLayout.CENTER);
		background.add(content, BorderLayout.CENTER);
		weatherView.favoriteButton.addActionListener(event -> {
			Place currentPlace = weatherView.favoritePlace();
			int favoriteIndex = weatherView.findFavorite(favoritePlaces);
			if (favoriteIndex >= 0) {
				favoritePlaces.remove(favoriteIndex);
				weatherView.favoriteButton.setLiked(false);
				favoritesButton.setText("Obľúbené (" + favoritePlaces.size() + ")");
			} else if (currentPlace != null) {
				favoritePlaces.add(currentPlace);
				weatherView.favoriteButton.setLiked(true);
				favoritesButton.setText("Obľúbené (" + favoritePlaces.size() + ")");
			}
		});
		Timer refreshTimer = new Timer(600000, event -> {
			if (!cityField.getText().trim().isEmpty() && searchButton.isEnabled()) {
				searchButton.doClick();
			}
		});
		refreshTimer.start();
		unitToggle.addActionListener(event -> {
			boolean fahrenheit = unitToggle.isSelected();
			unitToggle.setText(fahrenheit ? "°F" : "°C");
			weatherView.setFahrenheit(fahrenheit);
		});

		searchButton.addActionListener((ActionEvent event) -> {
			String city = cityField.getText().trim();
			if (city.isEmpty()) {
				return;
			}
			Place place = selectedPlace[0];
			selectedPlace[0] = null;
			searchButton.setEnabled(false);
			title.setText("Načítavam počasie pre " + city + "...");
			new SwingWorker<WeatherData, Void>() {
				@Override
				protected WeatherData doInBackground() throws Exception {
					return place != null && place.name().equals(city) ? loadWeather(place) : loadWeather(city);
				}

				@Override
				protected void done() {
					try {
						WeatherData weather = get();
						weatherView.update(weather);
						weatherView.favoriteButton.setLiked(weatherView.findFavorite(favoritePlaces) >= 0);
						title.setText("Počasie pre " + weather.location());
					} catch (Exception exception) {
						title.setText("Mesto sa nepodarilo nájsť");
						javax.swing.JOptionPane.showMessageDialog(frame,
								"Skontrolujte názov mesta a internetové pripojenie.",
								"Chyba načítania", javax.swing.JOptionPane.ERROR_MESSAGE);
					} finally {
						searchButton.setEnabled(true);
					}
				}
			}.execute();
		});
		cityField.addActionListener(searchButton.getActionListeners()[0]);
		cityField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent event) { refreshSuggestions(); }

			@Override
			public void removeUpdate(DocumentEvent event) { refreshSuggestions(); }

			@Override
			public void changedUpdate(DocumentEvent event) { refreshSuggestions(); }

			private void refreshSuggestions() {
				weatherView.favoriteButton.setLiked(false);
				selectedPlace[0] = null;
				String query = cityField.getText().trim();
				++suggestionRequest[0];
				if (query.length() < 2) {
					suggestionDelay.stop();
					suggestions.setVisible(false);
					return;
				}
				suggestionDelay.restart();
			}
		});
		suggestionDelay.addActionListener(event -> {
			String query = cityField.getText().trim();
			int request = suggestionRequest[0];
			new SwingWorker<List<Place>, Void>() {
				@Override
				protected List<Place> doInBackground() throws Exception {
					return loadSuggestions(query);
				}

				@Override
				protected void done() {
					if (request != suggestionRequest[0] || !query.equals(cityField.getText().trim())
							|| !cityField.isFocusOwner()) {
						return;
					}
					try {
						showSuggestions(get(), cityField, searchButton, suggestions, selectedPlace);
					} catch (Exception ignored) {
						suggestions.setVisible(false);
					}
				}
			}.execute();
		});

		frame.setContentPane(background);
		frame.setVisible(true);
		Timer backgroundAnimation = new Timer(120, event -> {
			background.advanceAnimation();
			background.repaint();
		});
		backgroundAnimation.start();
	}

	private static JPanel currentWeather() {
		return new WeatherView().currentWeather();
	}

	private static JPanel forecast() {
		return new WeatherView().forecast();
	}

	private static JPanel infoChip(String title, String value) {
		return infoChip(title, label(value, 13, INK, true));
	}

	private static JPanel infoChip(String title, JLabel valueLabel) {
		RoundedPanel chip = new RoundedPanel(new Color(30, 41, 54, 190));
		chip.setLayout(new javax.swing.BoxLayout(chip, javax.swing.BoxLayout.Y_AXIS));
		chip.setBorder(new EmptyBorder(12, 16, 12, 16));
		JLabel titleLabel = label(title.toUpperCase(Locale.ROOT), 10, MUTED, true);
		chip.add(titleLabel);
		chip.add(javax.swing.Box.createVerticalStrut(4));
		chip.add(valueLabel);
		return chip;
	}

	private static List<Place> loadSuggestions(String query) throws Exception {
		String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery
				+ "&count=5&language=sk&format=json";
		String json = getJson(url);
		List<Place> places = new ArrayList<>();
		Matcher matcher = Pattern.compile("\\{([^{}]*)\\}").matcher(json);
		while (matcher.find() && places.size() < 5) {
			String result = matcher.group();
			if (result.contains("\"name\"") && result.contains("\"latitude\"")) {
				String name = stringValue(result, "name");
				String region = stringValueOrEmpty(result, "admin1");
				String country = stringValueOrEmpty(result, "country");
				String description = name + (region.isEmpty() ? "" : ", " + region)
						+ (country.isEmpty() ? "" : ", " + country);
				places.add(new Place(name, description, numberValue(result, "latitude"),
						numberValue(result, "longitude")));
			}
		}
		return places;
	}

	private static void showSuggestions(List<Place> places, JTextField cityField,
			JButton searchButton, JPopupMenu suggestions, Place[] selectedPlace) {
		suggestions.removeAll();
		for (Place place : places) {
			JMenuItem item = new JMenuItem(place.description());
			item.setFont(UI);
			item.setForeground(INK);
			item.setBackground(POPUP_BACKGROUND);
			item.setBorder(new EmptyBorder(10, 14, 10, 14));
			item.setPreferredSize(new Dimension(Math.max(cityField.getWidth(), 320), 42));
			item.addActionListener(event -> {
				cityField.setText(place.name());
				selectedPlace[0] = place;
				suggestions.setVisible(false);
				searchButton.doClick();
			});
			suggestions.add(item);
		}
		if (!places.isEmpty()) {
			suggestions.show(cityField, 0, cityField.getHeight());
		} else {
			suggestions.setVisible(false);
		}
	}

	private static class Place {
		private final String name;
		private final String description;
		private final double latitude;
		private final double longitude;

		private Place(String name, String description, double latitude, double longitude) {
			this.name = name;
			this.description = description;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		private String name() { return name; }
		private String description() { return description; }
		private double latitude() { return latitude; }
		private double longitude() { return longitude; }
	}

	private static class WeatherView {
		private final JLabel location = label("Bratislava", 20, INK, true);
		private final JLabel date = label("Piatok, 5. september 2026", 13, MUTED, false);
		private final JLabel temperature = label("24°C", 66, INK, true);
		private final JLabel condition = label("Jasná obloha  ·  Pocitovo 25°", 14, new Color(194, 211, 219), false);
		private final WeatherIcon currentIcon = new WeatherIcon("☀");
		private final MetricRow wind = stat("Vietor", "→  12 km/h");
		private final MetricRow humidity = stat("Vlhkosť", "48 %");
		private final MetricRow visibility = stat("Viditeľnosť", "16 km");
		private final MetricRow pressure = stat("Tlak", "1013 hPa");
		private final MetricRow precipitation = stat("Zrážky", "0 mm");
		private final JLabel unitsValue = label("°C  ·  km/h", 13, INK, true);
		private final JLabel updatedValue = label("Práve teraz", 13, INK, true);
		private final JLabel sunValue = label("06:00  /  19:12", 13, INK, true);
		private final JLabel airValue = label("32 AQI  ·  PM2.5 1.5", 13, INK, true);
		private final JLabel tipValue = label("Príjemné počasie na von", 13, INK, true);
		private final HeartButton favoriteButton = new HeartButton();
		private final JPanel forecastCards = new JPanel(new GridLayout(1, 5, 12, 0));
		private final JPanel hourlyCards = new JPanel(new GridLayout(1, 8, 8, 0));
		private final TemperatureChart temperatureChart = new TemperatureChart();
		private boolean fahrenheit;
		private WeatherData latestWeather;

		private JPanel currentWeather() {
		RoundedPanel panel = new RoundedPanel(PANEL);
		panel.setLayout(new BorderLayout(20, 0));
		panel.setBorder(new EmptyBorder(26, 28, 26, 28));

		JPanel main = new JPanel(new BorderLayout(18, 0));
		main.setOpaque(false);
		currentIcon.setPreferredSize(new Dimension(110, 120));
		main.add(currentIcon, BorderLayout.WEST);
		JPanel details = new JPanel();
		details.setOpaque(false);
		details.setLayout(new javax.swing.BoxLayout(details, javax.swing.BoxLayout.Y_AXIS));
		details.add(location);
		details.add(date);
		temperature.setBorder(new EmptyBorder(8, 0, 0, 0));
		details.add(temperature);
		details.add(condition);
		main.add(details, BorderLayout.CENTER);
		styleButton(favoriteButton, new Color(47, 62, 78), INK);
		favoriteButton.setPreferredSize(new Dimension(52, 48));
		favoriteButton.setToolTipText("Pridať aktuálne mesto medzi obľúbené");
		JPanel mainWithFavorite = new JPanel(new BorderLayout(12, 0));
		mainWithFavorite.setOpaque(false);
		mainWithFavorite.add(main, BorderLayout.CENTER);
		mainWithFavorite.add(favoriteButton, BorderLayout.EAST);
		panel.add(mainWithFavorite, BorderLayout.CENTER);

		JPanel stats = new JPanel(new GridLayout(5, 1, 0, 9));
		stats.setOpaque(false);
		stats.setPreferredSize(new Dimension(150, 205));
		stats.setMinimumSize(new Dimension(150, 205));
		stats.add(wind);
		stats.add(humidity);
		stats.add(visibility);
		stats.add(pressure);
		stats.add(precipitation);
		panel.add(stats, BorderLayout.EAST);
		return panel;
	}

		private JPanel forecast() {
		JPanel area = new JPanel(new BorderLayout(0, 12));
		area.setOpaque(false);
		JLabel forecastTitle = label("Výhľad na 5 dní", 17, INK, true);
		forecastTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
		JPanel dailyForecast = new JPanel(new BorderLayout(0, 4));
		dailyForecast.setOpaque(false);
		dailyForecast.setPreferredSize(new Dimension(0, 185));
		dailyForecast.add(forecastTitle, BorderLayout.NORTH);
		forecastCards.setOpaque(false);
		String[] days = {"Dnes", "Sobota", "Nedeľa", "Pondelok", "Utorok"};
		String[] icons = {"☀", "⛅", "☁", "🌧", "☀"};
		String[] highs = {"24°C", "26°C", "21°C", "19°C", "23°C"};
		String[] lows = {"14°C", "15°C", "13°C", "12°C", "14°C"};
		for (int index = 0; index < days.length; index++) {
			forecastCards.add(dayCard(days[index], icons[index], highs[index], lows[index]));
		}
		dailyForecast.add(forecastCards, BorderLayout.CENTER);
		area.add(dailyForecast, BorderLayout.NORTH);
		JPanel hourly = new JPanel(new BorderLayout(0, 8));
		hourly.setOpaque(false);
		JLabel hourlyTitle = label("Najbližšie hodiny", 14, INK, true);
		hourlyTitle.setBorder(new EmptyBorder(8, 0, 0, 0));
		hourly.add(hourlyTitle, BorderLayout.NORTH);
		hourlyCards.setOpaque(false);
		refreshInitialHourly();
		temperatureChart.setData(new int[] {24, 24, 23, 23, 22, 22, 21, 21}, fahrenheit);
		hourly.add(hourlyCards, BorderLayout.CENTER);
		temperatureChart.setPreferredSize(new Dimension(0, 45));
		hourly.add(temperatureChart, BorderLayout.SOUTH);
		JPanel insights = new JPanel(new GridLayout(1, 5, 10, 0));
		insights.setOpaque(false);
		insights.add(infoChip("Aktualizácia", updatedValue));
		insights.add(infoChip("Slnko", sunValue));
		insights.add(infoChip("Vzduch", airValue));
		insights.add(infoChip("Odporúčanie", tipValue));
		insights.add(infoChip("Jednotky", unitsValue));
		JPanel bottom = new JPanel();
		bottom.setLayout(new javax.swing.BoxLayout(bottom, javax.swing.BoxLayout.Y_AXIS));
		bottom.setOpaque(false);
		hourly.setPreferredSize(new Dimension(0, 170));
		hourly.setMinimumSize(new Dimension(0, 170));
		hourly.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
		insights.setPreferredSize(new Dimension(0, 55));
		insights.setMinimumSize(new Dimension(0, 55));
		insights.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		bottom.add(hourly);
		bottom.add(javax.swing.Box.createVerticalStrut(10));
		bottom.add(insights);
		area.add(bottom, BorderLayout.CENTER);
		return area;
	}

		private void update(WeatherData weather) {
			latestWeather = weather;
			updatedValue.setText(weather.updatedAt());
			sunValue.setText(weather.sunrise() + "  /  " + weather.sunset());
			airValue.setText(weather.airQuality() + " AQI  ·  " + formatAir(weather.pm25()));
			tipValue.setText(weather.recommendation());
			currentIcon.setType(weather.currentIcon());
			location.setText(weather.location());
			date.setText(weather.date());
			refreshTemperatureLabels(weather);
			refreshMetrics(weather);
			refreshForecast(weather);
			refreshHourly(weather);
		}

		private void setFahrenheit(boolean fahrenheit) {
			this.fahrenheit = fahrenheit;
			unitsValue.setText(unit() + "  ·  " + windUnit());
			if (latestWeather != null) {
				refreshTemperatureLabels(latestWeather);
				refreshMetrics(latestWeather);
				refreshForecast(latestWeather);
				refreshHourly(latestWeather);
				temperatureChart.setData(latestWeather.hourlyTemperatures(), fahrenheit);
				temperatureChart.setData(latestWeather.hourlyTemperatures(), fahrenheit);
			} else {
				refreshInitialTemperatureLabels();
				refreshInitialMetrics();
				refreshInitialForecast();
				refreshInitialHourly();
				temperatureChart.setData(new int[] {24, 24, 23, 23, 22, 22, 21, 21}, fahrenheit);
			}
		}

		private void refreshTemperatureLabels(WeatherData weather) {
			temperature.setText(formatTemperature(weather.temperature()) + unit());
			condition.setText(weather.condition() + "  ·  Pocitovo "
					+ formatTemperature(weather.apparentTemperature()) + unit());
		}

		private int formatTemperature(int celsius) {
			return fahrenheit ? (int) Math.round(celsius * 9.0 / 5.0 + 32) : celsius;
		}

		private String unit() {
			return fahrenheit ? "°F" : "°C";
		}

		private String windUnit() {
			return fahrenheit ? "mph" : "km/h";
		}

		private String formatWind(int kilometersPerHour) {
			return fahrenheit
					? Math.round(kilometersPerHour * 0.621371) + " mph"
					: kilometersPerHour + " km/h";
		}

		private String formatVisibility(int kilometers) {
			return fahrenheit
					? String.format(Locale.ROOT, "%.1f mi", kilometers * 0.621371)
					: kilometers + " km";
		}

		private String formatPrecipitation(double millimeters) {
			return fahrenheit
					? String.format(Locale.ROOT, "%.2f in", millimeters * 0.0393701)
					: String.format(Locale.ROOT, "%.1f mm", millimeters);
		}

		private String formatAir(double pm25) {
			return String.format(Locale.ROOT, "PM2.5 %.1f", pm25);
		}

		private void refreshMetrics(WeatherData weather) {
			wind.setValue("→  " + formatWind(weather.wind()));
			humidity.setValue(weather.humidity() + " %");
			visibility.setValue(formatVisibility(weather.visibility()));
			pressure.setValue(formatPressure(weather.pressure()));
			precipitation.setValue(formatPrecipitation(weather.precipitation()));
		}

		private void refreshInitialMetrics() {
			wind.setValue("→  " + formatWind(12));
			humidity.setValue("48 %");
			visibility.setValue(formatVisibility(16));
			pressure.setValue(formatPressure(1013));
			precipitation.setValue(formatPrecipitation(0));
		}

		private String formatPressure(int hectopascals) {
			return fahrenheit
					? String.format(Locale.ROOT, "%.2f inHg", hectopascals * 0.029529983)
					: hectopascals + " hPa";
		}

		private void refreshInitialTemperatureLabels() {
			temperature.setText(formatTemperature(24) + unit());
			condition.setText("Jasná obloha  ·  Pocitovo " + formatTemperature(25) + unit());
		}

		private void refreshInitialForecast() {
			String[] days = {"Dnes", "Sobota", "Nedeľa", "Pondelok", "Utorok"};
			String[] icons = {"☀", "⛅", "☁", "🌧", "☀"};
			int[] highs = {24, 26, 21, 19, 23};
			int[] lows = {14, 15, 13, 12, 14};
			forecastCards.removeAll();
			for (int index = 0; index < days.length; index++) {
				forecastCards.add(dayCard(days[index], icons[index],
						formatTemperature(highs[index]) + unit(), formatTemperature(lows[index]) + unit()));
			}
			forecastCards.revalidate();
			forecastCards.repaint();
		}

		private void refreshInitialHourly() {
			String[] times = {"Teraz", "+1 h", "+2 h", "+3 h", "+4 h", "+5 h", "+6 h", "+7 h"};
			int[] temperatures = {24, 24, 23, 23, 22, 22, 21, 21};
			hourlyCards.removeAll();
			for (int index = 0; index < times.length; index++) {
				hourlyCards.add(hourCard(times[index], "☀", formatTemperature(temperatures[index]) + unit(), "0%"));
			}
			hourlyCards.revalidate();
			hourlyCards.repaint();
		}

		private void refreshForecast(WeatherData weather) {
			forecastCards.removeAll();
			for (int index = 0; index < weather.days().length; index++) {
				forecastCards.add(dayCard(weather.days()[index], weather.icons()[index],
						formatTemperature(weather.highs()[index]) + unit(), formatTemperature(weather.lows()[index]) + unit()));
			}
			forecastCards.revalidate();
			forecastCards.repaint();
		}

		private void refreshHourly(WeatherData weather) {
			hourlyCards.removeAll();
			for (int index = 0; index < weather.hourlyTimes().length; index++) {
				hourlyCards.add(hourCard(weather.hourlyTimes()[index], weatherIcon(weather.hourlyCodes()[index]),
						formatTemperature(weather.hourlyTemperatures()[index]) + unit(), weather.hourlyRain()[index] + "%"));
			}
			hourlyCards.revalidate();
			hourlyCards.repaint();
		}

		private Place favoritePlace() {
			if (latestWeather == null) {
				return new Place("Bratislava", "Bratislava, Slovensko", 48.1486, 17.1077);
			}
			return new Place(latestWeather.location(), latestWeather.location(),
					latestWeather.latitude(), latestWeather.longitude());
		}

		private int findFavorite(List<Place> favorites) {
			Place current = favoritePlace();
			if (current == null) return -1;
			for (int index = 0; index < favorites.size(); index++) {
				Place favorite = favorites.get(index);
				if (Math.abs(favorite.latitude() - current.latitude()) < 0.001
						&& Math.abs(favorite.longitude() - current.longitude()) < 0.001) {
					return index;
				}
			}
			return -1;
		}
	}

	private static WeatherData loadWeather(String city) throws Exception {
		String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
		String geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
				+ encodedCity + "&count=1&language=sk&format=json";
		String geocoding = getJson(geocodingUrl);
		if (!geocoding.contains("\"results\"")) {
			throw new IllegalArgumentException("Mesto nebolo nájdené");
		}

		double latitude = numberValue(geocoding, "latitude");
		double longitude = numberValue(geocoding, "longitude");
		String name = stringValue(geocoding, "name");
		String region = stringValueOrEmpty(geocoding, "admin1");
		String country = stringValueOrEmpty(geocoding, "country");
		String location = name + (region.isEmpty() ? "" : ", " + region)
				+ (country.isEmpty() ? "" : ", " + country);
		return loadWeatherAt(location, latitude, longitude);
	}

	private static WeatherData loadWeather(Place place) throws Exception {
		return loadWeatherAt(place.description(), place.latitude(), place.longitude());
	}

	private static WeatherData loadWeatherAt(String location, double latitude, double longitude) throws Exception {

		String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
				+ "&longitude=" + longitude
				+ "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,visibility,pressure_msl,precipitation"
				+ "&hourly=temperature_2m,weather_code,precipitation_probability,wind_speed_10m"
				+ "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset"
				+ "&timezone=auto&forecast_days=5";
		String weather = getJson(weatherUrl);
		String currentSection = section(weather, "current");
		String hourlySection = section(weather, "hourly");
		String dailySection = section(weather, "daily");
		int currentCode = (int) Math.round(numberValue(currentSection, "weather_code"));
		int[] dailyCodes = integerArray(dailySection, "weather_code");
		String[] dates = stringArray(dailySection, "time");
		String[] days = new String[dates.length];
		String[] icons = new String[dates.length];
		int[] highs = integerArray(dailySection, "temperature_2m_max");
		int[] lows = integerArray(dailySection, "temperature_2m_min");
		String[] hourlyTimes = stringArray(hourlySection, "time");
		int[] hourlyTemperatures = integerArray(hourlySection, "temperature_2m");
		int[] hourlyCodes = integerArray(hourlySection, "weather_code");
		int[] hourlyRain = integerArray(hourlySection, "precipitation_probability");
		int[] hourlyWind = integerArray(hourlySection, "wind_speed_10m");
		int currentHour = findValue(hourlyTimes, stringValue(currentSection, "time"));
		int hourlyCount = Math.min(8, hourlyTimes.length - currentHour);
		String[] nextHours = new String[hourlyCount];
		int[] nextTemperatures = new int[hourlyCount];
		int[] nextCodes = new int[hourlyCount];
		int[] nextRain = new int[hourlyCount];
		int[] nextWind = new int[hourlyCount];
		for (int index = 0; index < hourlyCount; index++) {
			int source = currentHour + index;
			nextHours[index] = hourLabel(hourlyTimes[source]);
			nextTemperatures[index] = hourlyTemperatures[source];
			nextCodes[index] = hourlyCodes[source];
			nextRain[index] = hourlyRain[source];
			nextWind[index] = hourlyWind[source];
		}
		String airQualityUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude="
				+ latitude + "&longitude=" + longitude + "&current=us_aqi,pm2_5,pm10&timezone=auto";
		String airQuality = getJson(airQualityUrl);
		String airCurrent = section(airQuality, "current");
		for (int index = 0; index < dates.length; index++) {
			days[index] = index == 0 ? "Dnes" : dayName(LocalDate.parse(dates[index]));
			icons[index] = weatherIcon(dailyCodes[index]);
		}

		return new WeatherData(location,
				latitude, longitude,
				(int) Math.round(numberValue(currentSection, "temperature_2m")),
				(int) Math.round(numberValue(currentSection, "apparent_temperature")),
				(int) Math.round(numberValue(currentSection, "wind_speed_10m")),
				(int) Math.round(numberValue(currentSection, "relative_humidity_2m")),
				(int) Math.round(numberValue(currentSection, "visibility") / 1000),
				condition(currentCode),
				weatherIcon(currentCode),
				(int) Math.round(numberValue(currentSection, "pressure_msl")),
				numberValue(currentSection, "precipitation"),
				formatUpdatedTime(stringValue(currentSection, "time")),
				firstStringArrayValue(dailySection, "sunrise"),
				firstStringArrayValue(dailySection, "sunset"),
				(int) Math.round(numberValue(airCurrent, "us_aqi")),
				numberValue(airCurrent, "pm2_5"),
				numberValue(airCurrent, "pm10"),
				recommendation(currentCode, (int) Math.round(numberValue(currentSection, "temperature_2m"))),
				nextHours, nextTemperatures, nextCodes, nextRain, nextWind,
				formatDate(LocalDate.parse(dates[0])), days, icons, highs, lows);
	}

	private static int findValue(String[] values, String target) {
		for (int index = 0; index < values.length; index++) {
			if (values[index].equals(target)) return index;
		}
		return 0;
	}

	private static String hourLabel(String time) {
		int separator = time.indexOf('T');
		return separator >= 0 ? time.substring(separator + 1, Math.min(time.length(), separator + 6)) : time;
	}

	private static String firstStringArrayValue(String json, String key) {
		String[] values = stringArray(json, key);
		return values.length == 0 ? "--:--" : hourLabel(values[0]);
	}

	private static String recommendation(int code, int temperature) {
		if (code >= 51 && code <= 99) return "Nezabudni na dáždnik";
		if (temperature >= 25) return "Ideálny deň na prechádzku";
		if (temperature <= 5) return "Hodí sa teplá bunda";
		return "Príjemné počasie na von";
	}

	private static String formatUpdatedTime(String time) {
		try {
			long minutes = Math.max(0, Duration.between(LocalDateTime.parse(time), LocalDateTime.now()).toMinutes());
			if (minutes < 1) {
				return "Práve teraz";
			}
			return minutes == 1 ? "Pred 1 min" : "Pred " + minutes + " min";
		} catch (RuntimeException exception) {
			return "Práve teraz";
		}
	}

	private static String getJson(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IllegalStateException("API vrátila HTTP " + response.statusCode());
		}
		return response.body();
	}

	private static String section(String json, String name) {
		int start = json.indexOf("\"" + name + "\"");
		int openingBrace = json.indexOf('{', start);
		int closingBrace = json.indexOf('}', openingBrace);
		return json.substring(openingBrace, closingBrace + 1);
	}

	private static String stringValue(String json, String key) {
		Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Chýba údaj " + key);
		}
		return matcher.group(1);
	}

	private static String stringValueOrEmpty(String json, String key) {
		Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
		return matcher.find() ? matcher.group(1) : "";
	}

	private static double numberValue(String json, String key) {
		Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Chýba údaj " + key);
		}
		return Double.parseDouble(matcher.group(1));
	}

	private static int[] integerArray(String json, String key) {
		String values = arrayValue(json, key);
		String[] parts = values.split(",");
		int[] result = new int[parts.length];
		for (int index = 0; index < parts.length; index++) {
			result[index] = (int) Math.round(Double.parseDouble(parts[index].trim()));
		}
		return result;
	}

	private static String[] stringArray(String json, String key) {
		String[] parts = arrayValue(json, key).split(",");
		for (int index = 0; index < parts.length; index++) {
			parts[index] = parts[index].trim().replace("\"", "");
		}
		return parts;
	}

	private static String arrayValue(String json, String key) {
		Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\[([^]]*)\\]").matcher(json);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Chýba pole " + key);
		}
		return matcher.group(1);
	}

	private static String formatDate(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.forLanguageTag("sk-SK")));
	}

	private static String dayName(LocalDate date) {
		String value = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("sk-SK")));
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static String condition(int code) {
		if (code == 0) return "Jasná obloha";
		if (code <= 3) return "Čiastočne oblačno";
		if (code <= 48) return "Hmla";
		if (code <= 67 || code >= 80 && code <= 82) return "Dážď";
		if (code <= 77 || code >= 85 && code <= 86) return "Sneženie";
		if (code >= 95) return "Búrka";
		return "Premenlivé počasie";
	}

	private static String weatherIcon(int code) {
		if (code == 0) return "☀";
		if (code <= 3) return "⛅";
		if (code <= 48) return "☁";
		if (code <= 67 || code >= 80 && code <= 82) return "🌧";
		if (code <= 77 || code >= 85 && code <= 86) return "❄";
		return "⛈";
	}

	private static class WeatherData {
		private final String location;
		private final double latitude;
		private final double longitude;
		private final int temperature;
		private final int apparentTemperature;
		private final int wind;
		private final int humidity;
		private final int visibility;
		private final String condition;
		private final String currentIcon;
		private final int pressure;
		private final double precipitation;
		private final String updatedAt;
		private final String sunrise;
		private final String sunset;
		private final int airQuality;
		private final double pm25;
		private final double pm10;
		private final String recommendation;
		private final String[] hourlyTimes;
		private final int[] hourlyTemperatures;
		private final int[] hourlyCodes;
		private final int[] hourlyRain;
		private final int[] hourlyWind;
		private final String date;
		private final String[] days;
		private final String[] icons;
		private final int[] highs;
		private final int[] lows;

		private WeatherData(String location, double latitude, double longitude, int temperature, int apparentTemperature, int wind,
				int humidity, int visibility, String condition, String currentIcon, int pressure,
				double precipitation, String updatedAt, String sunrise, String sunset, int airQuality,
				double pm25, double pm10, String recommendation, String[] hourlyTimes,
				int[] hourlyTemperatures, int[] hourlyCodes, int[] hourlyRain, int[] hourlyWind,
				String date, String[] days,
				String[] icons, int[] highs, int[] lows) {
			this.location = location;
			this.latitude = latitude;
			this.longitude = longitude;
			this.temperature = temperature;
			this.apparentTemperature = apparentTemperature;
			this.wind = wind;
			this.humidity = humidity;
			this.visibility = visibility;
			this.condition = condition;
			this.currentIcon = currentIcon;
			this.pressure = pressure;
			this.precipitation = precipitation;
			this.updatedAt = updatedAt;
			this.sunrise = sunrise;
			this.sunset = sunset;
			this.airQuality = airQuality;
			this.pm25 = pm25;
			this.pm10 = pm10;
			this.recommendation = recommendation;
			this.hourlyTimes = hourlyTimes;
			this.hourlyTemperatures = hourlyTemperatures;
			this.hourlyCodes = hourlyCodes;
			this.hourlyRain = hourlyRain;
			this.hourlyWind = hourlyWind;
			this.date = date;
			this.days = days;
			this.icons = icons;
			this.highs = highs;
			this.lows = lows;
		}

		private String location() { return location; }
		private double latitude() { return latitude; }
		private double longitude() { return longitude; }
		private int temperature() { return temperature; }
		private int apparentTemperature() { return apparentTemperature; }
		private int wind() { return wind; }
		private int humidity() { return humidity; }
		private int visibility() { return visibility; }
		private String condition() { return condition; }
		private String currentIcon() { return currentIcon; }
		private int pressure() { return pressure; }
		private double precipitation() { return precipitation; }
		private String updatedAt() { return updatedAt; }
		private String sunrise() { return sunrise; }
		private String sunset() { return sunset; }
		private int airQuality() { return airQuality; }
		private double pm25() { return pm25; }
		private double pm10() { return pm10; }
		private String recommendation() { return recommendation; }
		private String[] hourlyTimes() { return hourlyTimes; }
		private int[] hourlyTemperatures() { return hourlyTemperatures; }
		private int[] hourlyCodes() { return hourlyCodes; }
		private int[] hourlyRain() { return hourlyRain; }
		private int[] hourlyWind() { return hourlyWind; }
		private String date() { return date; }
		private String[] days() { return days; }
		private String[] icons() { return icons; }
		private int[] highs() { return highs; }
		private int[] lows() { return lows; }
	}

	private static JPanel dayCard(String day, String icon, String high, String low) {
		RoundedPanel card = new RoundedPanel(CARD);
		card.setLayout(new BorderLayout(0, 12));
		card.setBorder(new EmptyBorder(18, 12, 18, 12));
		JLabel dayLabel = label(day, 13, day.equals("Dnes") ? ACCENT : MUTED, true);
		dayLabel.setHorizontalAlignment(JLabel.CENTER);
		card.add(dayLabel, BorderLayout.NORTH);
		WeatherIcon weatherIcon = new WeatherIcon(icon);
		card.add(weatherIcon, BorderLayout.CENTER);
		JLabel degrees = label(high + "  /  " + low, 14, INK, true);
		degrees.setHorizontalAlignment(JLabel.CENTER);
		card.add(degrees, BorderLayout.SOUTH);
		return card;
	}

	private static JPanel hourCard(String time, String icon, String temperature, String rain) {
		RoundedPanel card = new RoundedPanel(new Color(30, 41, 54, 185));
		card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
		card.setBorder(new EmptyBorder(5, 6, 5, 6));
		JLabel timeLabel = label(time, 11, MUTED, true);
		timeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		WeatherIcon weatherIcon = new WeatherIcon(icon);
		weatherIcon.setPreferredSize(new Dimension(46, 30));
		weatherIcon.setMaximumSize(new Dimension(46, 30));
		JLabel temperatureLabel = label(temperature, 12, INK, true);
		temperatureLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		JLabel rainLabel = label("Zrážky " + rain, 10, new Color(117, 190, 229), false);
		rainLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		card.add(timeLabel);
		card.add(weatherIcon);
		card.add(temperatureLabel);
		card.add(rainLabel);
		return card;
	}

	private static class WeatherIcon extends JPanel {
		private String type;

		WeatherIcon(String type) {
			this.type = type;
			setOpaque(false);
			setPreferredSize(new Dimension(80, 92));
		}

		void setType(String type) {
			this.type = type;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int centerX = getWidth() / 2;
			int centerY = getHeight() / 2;
			double scale = Math.min(0.58, Math.min(getWidth() / 80.0, getHeight() / 92.0) * 0.82);
			g.translate(centerX, centerY);
			g.scale(scale, scale);
			g.translate(-centerX, -centerY);
			if ("☀".equals(type)) {
				g.setColor(new Color(255, 194, 92));
				g.fillOval(centerX - 18, centerY - 18, 36, 36);
				g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				for (int angle = 0; angle < 360; angle += 45) {
					double radians = Math.toRadians(angle);
					int startX = centerX + (int) (Math.cos(radians) * 26);
					int startY = centerY + (int) (Math.sin(radians) * 26);
					int endX = centerX + (int) (Math.cos(radians) * 34);
					int endY = centerY + (int) (Math.sin(radians) * 34);
					g.drawLine(startX, startY, endX, endY);
				}
			} else {
				g.setColor(new Color(143, 174, 194));
				g.fillOval(centerX - 30, centerY - 2, 42, 25);
				g.fillOval(centerX - 8, centerY - 18, 34, 38);
				g.fillRoundRect(centerX - 34, centerY + 7, 68, 18, 10, 10);
				if ("🌧".equals(type)) {
					g.setColor(new Color(107, 183, 232));
					g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					for (int offset = -20; offset <= 20; offset += 20) {
						g.drawLine(centerX + offset, centerY + 34, centerX + offset - 5, centerY + 45);
					}
				} else if ("❄".equals(type)) {
					g.setColor(new Color(174, 222, 244));
					g.setStroke(new BasicStroke(2));
					for (int offset = -18; offset <= 18; offset += 18) {
						g.drawLine(centerX + offset, centerY + 34, centerX + offset, centerY + 45);
						g.drawLine(centerX + offset - 4, centerY + 39, centerX + offset + 4, centerY + 39);
					}
				}
			}
			g.dispose();
		}
	}

	private static class TemperatureChart extends JPanel {
		private int[] celsiusValues = new int[0];
		private boolean fahrenheit;

		TemperatureChart() {
			setOpaque(false);
		}

		void setData(int[] values, boolean fahrenheit) {
			this.celsiusValues = values.clone();
			this.fahrenheit = fahrenheit;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			if (celsiusValues.length < 2) return;
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int left = 14;
			int right = getWidth() - 14;
			int top = 10;
			int bottom = getHeight() - 14;
			int[] values = new int[celsiusValues.length];
			int minimum = Integer.MAX_VALUE;
			int maximum = Integer.MIN_VALUE;
			for (int index = 0; index < values.length; index++) {
				values[index] = fahrenheit ? (int) Math.round(celsiusValues[index] * 9.0 / 5.0 + 32) : celsiusValues[index];
				minimum = Math.min(minimum, values[index]);
				maximum = Math.max(maximum, values[index]);
			}
			if (maximum == minimum) maximum++;
			g.setColor(new Color(130, 153, 170, 45));
			g.drawLine(left, bottom, right, bottom);
			g.drawLine(left, top, left, bottom);
			g.setColor(ACCENT);
			g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int previousX = left;
			int previousY = bottom - (values[0] - minimum) * (bottom - top) / (maximum - minimum);
			for (int index = 1; index < values.length; index++) {
				int x = left + index * (right - left) / (values.length - 1);
				int y = bottom - (values[index] - minimum) * (bottom - top) / (maximum - minimum);
				g.drawLine(previousX, previousY, x, y);
				g.fillOval(x - 3, y - 3, 6, 6);
				previousX = x;
				previousY = y;
			}
			g.dispose();
		}
	}

	private static MetricRow stat(String name, String value) {
		return new MetricRow(name, value);
	}

	private static class MetricRow extends JPanel {
		private final JLabel valueLabel;

		MetricRow(String name, String value) {
			setOpaque(false);
			setLayout(new BorderLayout(12, 0));
			setPreferredSize(new Dimension(150, 30));
			setMinimumSize(new Dimension(150, 30));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
			JLabel nameLabel = label(name, 12, MUTED, false);
			valueLabel = label(value, 13, INK, true);
			valueLabel.setHorizontalAlignment(JLabel.RIGHT);
			add(nameLabel, BorderLayout.WEST);
			add(valueLabel, BorderLayout.CENTER);
		}

		void setValue(String value) {
			valueLabel.setText(value);
		}
	}

	private static JLabel label(String text, int size, Color color, boolean bold) {
		JLabel result = new JLabel(text);
		result.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
		result.setForeground(color);
		return result;
	}

	private static void styleButton(JButton button, Color background, Color foreground) {
		button.setFont(UI_BOLD);
		button.setForeground(foreground);
		button.setBackground(background);
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setBorder(new EmptyBorder(10, 16, 10, 16));
		button.setPreferredSize(new Dimension(98, 48));
		button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
	}

	private static void styleToggle(JToggleButton toggle) {
		toggle.setFont(UI_BOLD);
		toggle.setForeground(INK);
		toggle.setBackground(new Color(47, 62, 78));
		toggle.setFocusPainted(false);
		toggle.setContentAreaFilled(false);
		toggle.setOpaque(false);
		toggle.setBorder(new EmptyBorder(10, 13, 10, 13));
		toggle.setPreferredSize(new Dimension(52, 48));
		toggle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
	}

	private static class PlaceholderField extends JTextField {
		private final String placeholder;

		PlaceholderField(String placeholder) {
			this.placeholder = placeholder;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			if (getText().isEmpty() && !isFocusOwner()) {
				Graphics2D g = (Graphics2D) graphics.create();
				g.setColor(MUTED);
				g.setFont(getFont());
				int baseline = (getHeight() - getFontMetrics(getFont()).getHeight()) / 2
						+ getFontMetrics(getFont()).getAscent();
				g.drawString(placeholder, getInsets().left, baseline);
				g.dispose();
			}
		}
	}

	private static class HeartButton extends JButton {
		private boolean liked;

		HeartButton() {
			super();
			setRolloverEnabled(true);
		}

		void setLiked(boolean liked) {
			this.liked = liked;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getModel().isRollover() ? new Color(65, 82, 100) : getBackground());
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
			g.setColor(liked ? new Color(255, 112, 135) : MUTED);
			int centerX = getWidth() / 2;
			int top = getHeight() / 2 - 9;
			g.fillOval(centerX - 13, top, 14, 14);
			g.fillOval(centerX - 1, top, 14, 14);
			int[] xPoints = {centerX - 14, centerX + 14, centerX};
			int[] yPoints = {top + 8, top + 8, top + 25};
			g.fillPolygon(xPoints, yPoints, 3);
			g.dispose();
		}
	}

	private static class RoundedButton extends JButton {
		RoundedButton(String text) {
			super(text);
			setRolloverEnabled(true);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color color = getBackground();
			if (getModel().isRollover()) {
				color = color.brighter();
			}
			g.setColor(color);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	private static class RoundedToggleButton extends JToggleButton {
		RoundedToggleButton(String text) {
			super(text);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(isSelected() ? ACCENT : getBackground());
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	private static class RoundedPanel extends JPanel {
		private final Color color;

		RoundedPanel(Color color) {
			this.color = color;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(color);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
			g.setColor(new Color(255, 255, 255, 25));
			g.setStroke(new BasicStroke(1));
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	private static class GradientPanel extends JPanel {
		private int animationPhase;

		GradientPanel() {
			setOpaque(false);
		}

		void advanceAnimation() {
			animationPhase = (animationPhase + 1) % 360;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setPaint(new GradientPaint(0, 0, new Color(12, 18, 27), getWidth(), getHeight(), new Color(28, 43, 58)));
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(new Color(255, 192, 93, 12));
			g.fillOval(getWidth() - 180, -130, 310, 310);
			g.setColor(new Color(166, 199, 216, 22));
			int drift = (int) (Math.sin(Math.toRadians(animationPhase)) * 8);
			g.fillOval(getWidth() - 270 + drift, getHeight() - 190, 170, 72);
			g.fillOval(getWidth() - 175 + drift, getHeight() - 220, 145, 105);
			g.fillOval(24 - drift, getHeight() - 120, 130, 56);
			g.fillOval(98 - drift, getHeight() - 142, 108, 78);
			g.setColor(new Color(108, 174, 211, 28));
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (int offset = 0; offset < 5; offset++) {
				int x = getWidth() - 210 + offset * 22;
				g.drawLine(x, getHeight() - 90, x - 7, getHeight() - 70);
			}
			g.dispose();
		}
	}
}
