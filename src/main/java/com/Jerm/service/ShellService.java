package com.Jerm.service;

import com.Jerm.model.CommandResult;

public interface ShellService {
    /**
     * Executes a given command string.
     * @param command The command to execute.
     * @return a CommandResult containing the output, error, and exit code.
     */
    CommandResult executeCommand(String command);
}