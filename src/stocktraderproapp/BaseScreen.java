package stocktraderproapp;

import javafx.scene.Parent;

public abstract class BaseScreen implements AppScreen {

    protected final ScreenManager manager;

    public BaseScreen(ScreenManager manager) {
        this.manager = manager;
    }

    @Override
    public abstract Parent getView();
}
