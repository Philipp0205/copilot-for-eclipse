// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.UiConstants;

/**
 * Opens the Copilot Sessions overview.
 */
public class OpenCopilotSessionsHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null && window.getActivePage() != null) {
      try {
        window.getActivePage().showView(UiConstants.COPILOT_SESSIONS_VIEW_ID);
      } catch (PartInitException e) {
        CopilotCore.LOGGER.error("Failed to open Copilot Sessions", e);
      }
    }
    return null;
  }
}
