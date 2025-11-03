package com.Jerm;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.Jerm.model.CommandResult;
import com.Jerm.service.FakeShellServiceImpl;
import com.Jerm.service.ShellService;
import java.io.IOException;
import java.util.List;

public class Jerm {
    public static void main(String[] args) {
        // Apps components
        ShellService shellService = new FakeShellServiceImpl();

        try (Terminal terminal = new DefaultTerminalFactory().createTerminal()) {
            Screen screen = new TerminalScreen(terminal);
            screen.startScreen();

            // UI (View)
            BasicWindow window = new BasicWindow("Jerm Terminal (Running in WSL)");
            window.setHints(List.of(Window.Hint.EXPANDED));

            Panel mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            Label outputArea = new Label("Welcome! Enter a command below.");
            outputArea.setPreferredSize(new TerminalSize(80, 10));
            TextBox commandInput = new TextBox().setPreferredSize(new TerminalSize(70, 1));

            Button executeButton = new Button("Execute", () -> {
                // After User action (Controller)
                String command = commandInput.getText();
                if (command.isBlank()) return; // Do nothing if input is empty

                // Delegate the work to service
                CommandResult result = shellService.executeCommand(command);
                // Update the up with the result
                outputArea.setText(result.output());
                commandInput.setText(""); // Clear the input box
            });

            // UI Component Assembly
            Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            inputPanel.addComponent(new Label("> "));
            inputPanel.addComponent(commandInput);

            mainPanel.addComponent(outputArea.withBorder(Borders.singleLine("Output")));
            mainPanel.addComponent(inputPanel.withBorder(Borders.singleLine("Command")));
            mainPanel.addComponent(executeButton);
            window.setComponent(mainPanel);

            // Start the GUI
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            gui.addWindowAndWait(window);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}