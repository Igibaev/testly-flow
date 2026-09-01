package com.testlyflow.ui.admin;

import com.testlyflow.ui.MainLayout;
import com.testlyflow.ui.support.AdminSession;
import com.testlyflow.ui.support.NativeUi;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

@Route(value = "admin/login", layout = MainLayout.class)
@PageTitle("Вход в админ-панель")
public class AdminLoginView extends Div {

    private final String expectedPassword;
    private final Input password = new Input();
    private final Div errorBox = new Div();

    public AdminLoginView(@Value("${app.admin.password}") String expectedPassword) {
        this.expectedPassword = expectedPassword;
        addClassName("card");
        addClassName("password-gate");
        add(new H2("Вход в админ-панель"));

        errorBox.addClassName("error-box");
        errorBox.setVisible(false);
        add(errorBox);

        password.setId("admin-password");
        password.getElement().setAttribute("type", "password");
        NativeLabel label = new NativeLabel();
        label.addClassName("form-field");
        label.add(new Span("Пароль"), password);
        add(label);
        add(NativeUi.button("Войти", e -> handleLogin(), "btn"));
    }

    private void handleLogin() {
        String provided = password.getValue() == null ? "" : password.getValue();
        if (!AdminSession.passwordMatches(provided, expectedPassword)) {
            errorBox.setText("Неверный пароль администратора");
            errorBox.setVisible(true);
            return;
        }
        AdminSession.authorize();
        String redirect = AdminSession.takeRedirect();
        getUI().ifPresent(ui -> {
            try {
                if (redirect == null) {
                    ui.navigate(AdminCategoriesView.class);
                } else {
                    ui.navigate(redirect);
                }
            } catch (com.vaadin.flow.router.NotFoundException ignored) {
                // Tests may run without a full route registry; authorization already succeeded.
            }
        });
    }
}
