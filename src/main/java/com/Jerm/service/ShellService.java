package com.Jerm.service;

import com.Jerm.model.CommandResult;
import java.io.Closeable;

public interface ShellService extends Closeable {
    /**
     * Executes a given command string in the persistent session.
     * @param command The command to execute.
     * @return a CommandResult containing the output.
     */
    CommandResult executeCommand(String command);
}