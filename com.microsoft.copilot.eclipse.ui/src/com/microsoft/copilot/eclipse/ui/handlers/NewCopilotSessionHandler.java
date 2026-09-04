// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Creates an additional independent Copilot chat view.
 */
public class NewCopilotSessionHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    SessionViewOpener.openNewSession();
    return null;
  }
}
