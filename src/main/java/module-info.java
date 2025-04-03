module com.example.ti2mark1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.ti2mark1 to javafx.fxml;
    exports com.example.ti2mark1;
}