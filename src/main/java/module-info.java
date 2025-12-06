module org.example.vp_final {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.controlsfx.controls; // <-- ЭТО БЫЛА ОШИБКА, ИСПРАВЛЕНО НА controlsfx
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires jaudiotagger;
    requires javafx.media;

    opens org.example.vp_final to javafx.fxml;
    exports org.example.vp_final;
}