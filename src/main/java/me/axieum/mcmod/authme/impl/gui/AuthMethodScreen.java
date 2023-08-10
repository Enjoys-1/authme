package me.axieum.mcmod.authme.impl.gui;

import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import me.axieum.mcmod.authme.api.util.SessionUtils;
import me.axieum.mcmod.authme.impl.AuthMe;
import me.axieum.mcmod.authme.impl.config.AuthMeConfig;
import static me.axieum.mcmod.authme.impl.AuthMe.CONFIG;
import static me.axieum.mcmod.authme.impl.AuthMe.WIDGETS_TEXTURE;
import static me.axieum.mcmod.authme.impl.AuthMe.getConfig;

/**
 * A screen for choosing a user authentication method.
 */
public class AuthMethodScreen extends Screen
{
    // The parent (or last) screen that opened this screen
    private final Screen parentScreen;

    // The 'Microsoft' authentication method button textures
    public static final ButtonTextures MICROSOFT_BUTTON_TEXTURES = new ButtonTextures(
        Identifier.of("authme", "widget/microsoft_button"),
        Identifier.of("authme", "widget/microsoft_button_disabled"),
        Identifier.of("authme", "widget/microsoft_button_focused")
    );
    // The 'Mojang (or legacy)' authentication method button textures
    public static final ButtonTextures MOJANG_BUTTON_TEXTURES = new ButtonTextures(
        Identifier.of("authme", "widget/mojang_button"),
        Identifier.of("authme", "widget/mojang_button_disabled"),
        Identifier.of("authme", "widget/mojang_button_focused")
    );
    // The 'Offline' authentication method button textures
    public static final ButtonTextures OFFLINE_BUTTON_TEXTURES = new ButtonTextures(
        Identifier.of("authme", "widget/offline_button"),
        Identifier.of("authme", "widget/offline_button_disabled"),
        Identifier.of("authme", "widget/offline_button_focused")
    );

    /**
     * Constructs a new authentication method choice screen.
     *
     * @param parentScreen parent (or last) screen that opened this screen
     */
    public AuthMethodScreen(Screen parentScreen)
    {
        super(Text.translatable("gui.authme.method.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init()
    {
        super.init();
        assert client != null;

        // Add a title
        TextWidget titleWidget = new TextWidget(width, height, title, textRenderer);
        titleWidget.setTextColor(0xffffff);
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, height / 2 - titleWidget.getHeight() / 2 - 22);
        addDrawableChild(titleWidget);

        // Add a greeting message
        TextWidget greetingWidget = new TextWidget(
            width, height,
            Text.translatable(
                "gui.authme.method.greeting",
                Text.literal(SessionUtils.getSession().getUsername()).formatted(Formatting.YELLOW)
            ),
            textRenderer
        );
        greetingWidget.setTextColor(0xa0a0a0);
        greetingWidget.setPosition(
            width / 2 - greetingWidget.getWidth() / 2, height / 2 - greetingWidget.getHeight() / 2 - 42
        );
        addDrawableChild(greetingWidget);

        // Add a button for the 'Microsoft' authentication method
        TexturedButtonWidget msButton = new TexturedButtonWidget(
            width / 2 - 20 - 10 - 4, height / 2 - 5, 20, 20,
            MICROSOFT_BUTTON_TEXTURES,
            button -> {
                // If 'Left Control' is being held, enforce user interaction
                final boolean selectAccount = InputUtil.isKeyPressed(
                    client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_CONTROL
                );
                if (getConfig().methods.microsoft.isDefaults()) {
                    saveSessionWarning(new MicrosoftAuthScreen(this, parentScreen, selectAccount));
                } else {
                    AuthMe.LOGGER.warn("Non-default Microsoft authentication URLs are in use!");
                    ConfirmScreen confirmScreen = new ConfirmScreen(
                        accepted -> {
                            if (accepted) {
                                saveSessionWarning(new MicrosoftAuthScreen(this, parentScreen, selectAccount));
                            } else {
                                client.setScreen(this);
                            }
                        },
                        Text.translatable("gui.authme.microsoft.warning.title"),
                        Text.translatable("gui.authme.microsoft.warning.body"),
                        Text.translatable("gui.authme.microsoft.warning.accept"),
                        Text.translatable("gui.authme.microsoft.warning.cancel")
                    );
                    client.setScreen(confirmScreen);
                    confirmScreen.disableButtons(40);
                }
            },
            Text.translatable("gui.authme.method.button.microsoft")
        );
        msButton.setTooltip(Tooltip.of(
            Text.translatable("gui.authme.method.button.microsoft")
                .append("\n")
                .append(
                    Text.translatable("gui.authme.method.button.microsoft.selectAccount").formatted(Formatting.GRAY)
                )
        ));
        addDrawableChild(msButton);

        // Add a button for the 'Mojang (or legacy)' authentication method
        TexturedButtonWidget mojangButton = new TexturedButtonWidget(
            width / 2 - 10, height / 2 - 5, 20, 20,
            MOJANG_BUTTON_TEXTURES,
            ConfirmLinkScreen.opening(this, AuthMe.MOJANG_ACCOUNT_MIGRATION_FAQ_URL),
            Text.translatable("gui.authme.method.button.mojang")
        );
        mojangButton.setTooltip(Tooltip.of(
            Text.translatable("gui.authme.method.button.mojang")
                .append("\n")
                .append(Text.translatable("gui.authme.method.button.mojang.unavailable").formatted(Formatting.RED))
        ));
        addDrawableChild(mojangButton);

        // Add a button for the 'Offline' authentication method
        TexturedButtonWidget offlineButton = new TexturedButtonWidget(
            width / 2 + 10 + 4, height / 2 - 5, 20, 20,
            OFFLINE_BUTTON_TEXTURES,
            button -> client.setScreen(new OfflineAuthScreen(this, parentScreen)),
            Text.translatable("gui.authme.method.button.offline")
        );
        offlineButton.setTooltip(Tooltip.of(Text.translatable("gui.authme.method.button.offline")));
        addDrawableChild(offlineButton);

        // Add a button to login using saved session if one exists
        boolean hasSavedSession = getConfig().autoLogin.savedSession.hasSavedSession();
        if (hasSavedSession) {
            addDrawableChild(
                ButtonWidget.builder(Text.translatable("gui.authme.savedSession.button"), button -> {
                    Session session = getConfig().autoLogin.savedSession.getSession();
                    SessionUtils.setSession(session);
                    // Add a toast that greets the player
                    SystemToast.add(
                        client.getToastManager(), SystemToast.Type.TUTORIAL_HINT,
                        Text.translatable("gui.authme.toast.greeting", Text.literal(session.getUsername())), null
                    );
                    close();
                })
                .dimensions(width / 2 - 50, height / 2 + 27, 100, 20)
                .build()
            );
        }

        // Add a button to go back
        addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.back"), button -> close())
                .dimensions(width / 2 - 50, height / 2 + 27 + (hasSavedSession ? 27 : 0), 100, 20)
                .build()
        );
    }

    @Override
    public void close()
    {
        if (client != null) client.setScreen(parentScreen);
    }

    public void saveSessionWarning(Screen nextScreen)
    {
        assert client != null;
        AuthMeConfig.AutoLoginSchema autoLogin = getConfig().autoLogin;
        if (autoLogin.saveSession && !autoLogin.warningScreenConfirmed) {
            ConfirmScreen confirmScreen = new ConfirmScreen(
                accepted -> {
                    if (accepted) {
                        client.setScreen(nextScreen);
                        autoLogin.warningScreenConfirmed = true;
                        CONFIG.save();
                    } else {
                        client.setScreen(this);
                    }
                },
                Text.translatable("gui.authme.autoLogin.warning.title"),
                Text.translatable("gui.authme.autoLogin.warning.body"),
                Text.translatable("gui.authme.autoLogin.warning.accept"),
                Text.translatable("gui.authme.autoLogin.warning.cancel")
            );
            client.setScreen(confirmScreen);
            confirmScreen.disableButtons(40);
        } else {
            client.setScreen(nextScreen);
        }
    }
}
