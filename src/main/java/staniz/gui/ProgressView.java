package staniz.gui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import staniz.model.WorkoutData.Exercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.service.WorkoutService;

/**
 * Shows exact recorded performance with explicitly labelled per-session weight and rep summaries.
 */
public class ProgressView extends VBox {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    /**
     * Builds the progress selector, session details, and charts when at least two recordings exist.
     */
    public ProgressView(WorkoutService service) {
        super(16);
        getStyleClass().add("page");
        Label heading = new Label("Exercise progress");
        heading.getStyleClass().add("page-title");
        Label help = new Label("Exact recorded sets only. As-planned sessions are excluded from comparisons.");
        help.setWrapText(true);
        // The selector holds library entries rather than their names, so the chosen exercise carries
        // its own stable ID and no display string has to be resolved back into an identity.
        ComboBox<Exercise> exercise = new ComboBox<>();
        exercise.setId("progress-exercise");
        exercise.setPromptText("Choose an exercise");
        exercise.setMaxWidth(Double.MAX_VALUE);
        exercise.setConverter(new StringConverter<>() {
            @Override
            public String toString(Exercise item) {
                return item == null ? "" : item.name();
            }

            @Override
            public Exercise fromString(String text) {
                return null; // The selector is not editable, so text is never converted back.
            }
        });
        exercise.getItems().setAll(service.getState().library());
        VBox results = new VBox(14);
        results.setId("progress-results");
        exercise.valueProperty().addListener((observable, before, after) -> {
            results.getChildren().clear();
            if (after == null) {
                return;
            }
            String id = after.id();
            List<WorkoutRecord> records = service.performance(id);
            results.getChildren().add(new Label(MainWindow.quantity(records.size(), "exact recorded session")));
            if (records.size() >= 2) {
                results.getChildren().addAll(chart(records, id, true), chart(records, id, false));
            } else {
                results.getChildren().add(new Label("Record this exercise in two sessions to see its charts."));
            }
            for (int index = records.size() - 1; index >= 0; index--) {
                WorkoutRecord record = records.get(index);
                Label row = new Label("Session " + (index + 1) + " · "
                        + DATE.format(Instant.parse(record.completedAt())) + " · " + record.dayName()
                        + "\n" + describe(record, id));
                row.setWrapText(true);
                row.setMaxWidth(Double.MAX_VALUE);
                row.getStyleClass().add("card");
                results.getChildren().add(row);
            }
        });
        getChildren().addAll(heading, help, exercise, results);
        if (!exercise.getItems().isEmpty()) {
            exercise.getSelectionModel().selectFirst();
        }
    }

    /**
     * Formats the latest actual sets for an exercise without presenting prescribed ranges as measurements.
     */
    public static String previous(WorkoutService service, String exerciseId) {
        return service.previousPerformance(exerciseId)
                .map(record -> "Last recorded · " + DATE.format(Instant.parse(record.completedAt())) + "\n"
                        + describe(record, exerciseId)).orElse("No exact performance recorded yet.");
    }

    private static String describe(WorkoutRecord record, String id) {
        return sets(record, id).stream().map(set -> set.kg() + " kg × " + set.minReps() + " reps")
                .collect(Collectors.joining("  |  "));
    }

    private static List<WorkoutSet> sets(WorkoutRecord record, String id) {
        return record.exercises().stream().filter(exercise -> exercise.exerciseId().equals(id))
                .flatMap(exercise -> exercise.sets().stream()).toList();
    }

    private static LineChart<String, Number> chart(List<WorkoutRecord> records, String id, boolean weight) {
        CategoryAxis horizontal = new CategoryAxis();
        horizontal.setLabel("Recorded session (chronological)");
        NumberAxis vertical = new NumberAxis();
        vertical.setLabel(weight ? "Heaviest set (kg)" : "Total recorded reps");
        LineChart<String, Number> chart = new LineChart<>(horizontal, vertical);
        chart.setId(weight ? "weight-chart" : "reps-chart");
        chart.setTitle(weight ? "Heaviest recorded set per session" : "Recorded rep totals per session");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        chart.setMinHeight(260);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int index = 0; index < records.size(); index++) {
            List<WorkoutSet> values = sets(records.get(index), id);
            double value = weight ? values.stream().mapToDouble(WorkoutSet::kg).max().orElse(0)
                    : values.stream().mapToLong(WorkoutSet::minReps).sum();
            series.getData().add(new XYChart.Data<>(Integer.toString(index + 1), value));
        }
        chart.getData().add(series);
        return chart;
    }
}
