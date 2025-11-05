package com.Jerm;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.Jerm.model.CommandResult;
import com.Jerm.service.ShellService;
import com.Jerm.service.ShellServiceImpl;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Jerm {

    static void main() {
        try (ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
             ShellService shellService = new ShellServiceImpl()) {

            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            Screen screen = new TerminalScreen(terminal);
            screen.startScreen();

            final String ansiRegex = "\\u001B\\[[;\\d]*[ -/]*[@-~]";
            BasicWindow window = new BasicWindow("Jerm Terminal (Real Session)");
            window.setHints(List.of(Window.Hint.EXPANDED));

            Panel mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            Label outputArea = new Label("Welcome to Jerm. Your persistent session is ready.");
            outputArea.setPreferredSize(new TerminalSize(80, 15));
            TextBox commandInput = new TextBox().setPreferredSize(new TerminalSize(70, 1));

            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);

            final Button[] executeButtonHolder = new Button[1];

            Button executeButton = new Button("Execute", () -> {
                String command = commandInput.getText();
                if (command.isBlank()) return;

                commandInput.setText("");
                outputArea.setText("Executing '" + command + "'...");
                executeButtonHolder[0].setEnabled(false);

                // Invalidate the component after setting its text to signal
                // to the GUI that it needs to be redrawn immediately
                outputArea.invalidate();

                commandExecutor.submit(() -> {
                    try {
                        CommandResult result = shellService.executeCommand(command);
                        String sanitizedOutput = result.output().replaceAll(ansiRegex, "").trim();

                        gui.getGUIThread().invokeLater(() -> {
                            if (!sanitizedOutput.isEmpty() || result.exitCode() != 0) {
                                outputArea.setText(sanitizedOutput);
                            } else {
                                outputArea.setText("Done.");
                            }

                            // Invalidate again after the final update
                            outputArea.invalidate();
                            executeButtonHolder[0].setEnabled(true);
                        });
                    } catch (Exception e) {
                        gui.getGUIThread().invokeLater(() -> {
                            outputArea.setText("Error: " + e.getMessage());
                            outputArea.invalidate(); // Also invalidate on error
                            executeButtonHolder[0].setEnabled(true);
                        });
                    }
                });
            });

            executeButtonHolder[0] = executeButton;

            // Assemble the UI
            Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            inputPanel.addComponent(new Label("> "));
            inputPanel.addComponent(commandInput);
            mainPanel.addComponent(outputArea.withBorder(Borders.singleLine("Output")));
            mainPanel.addComponent(inputPanel.withBorder(Borders.singleLine("Command")));
            mainPanel.addComponent(executeButton);
            window.setComponent(mainPanel);

            gui.addWindowAndWait(window);

        } catch (Exception e) {
            System.err.println("A critical error occurred:");
            e.printStackTrace();
        }
    }
}