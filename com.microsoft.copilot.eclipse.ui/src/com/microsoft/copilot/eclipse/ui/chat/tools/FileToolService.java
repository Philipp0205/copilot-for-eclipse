// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.ChatSessionEvent;
import com.microsoft.copilot.eclipse.ui.chat.WorkingSetBar;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatBaseService;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSession;
import com.microsoft.copilot.eclipse.ui.chat.services.TodoListService;

/**
 * Service for the Edit File tool. This service manages the state of the Create File Tool and Edit File tool, including
 * the files to be created or edited and the enable state of the button.
 */
public class FileToolService extends ChatBaseService {
  private final Map<String, SessionFileState> sessionStates = new ConcurrentHashMap<>();
  private final ThreadLocal<String> invocationSession = new ThreadLocal<>();
  private CreateFileTool createFileTool;
  private EditFileTool editFileTool;

  /**
   * Constructor for FileToolService.
   */
  public FileToolService(CopilotLanguageServerConnection lsConnection) {
    super(lsConnection, null);

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_NEW_CONVERSATION, event -> {
      SessionFileState state = sessionStates.get(ChatSessionEvent.sessionId(event));
      if (state != null) {
        onResolveAllChanges(state);
      }
    });
  }

  /**
   * Bind the WorkingSetBar to the changed files.
   */
  public void bindWorkingSetBar(ChatView chatView) {
    if (this.createFileTool == null) {
      this.createFileTool = (CreateFileTool) CopilotUi.getPlugin().getChatServiceManager().getAgentToolService()
          .getTool(CreateFileTool.TOOL_NAME);
    }
    if (this.editFileTool == null) {
      this.editFileTool = (EditFileTool) CopilotUi.getPlugin().getChatServiceManager().getAgentToolService()
          .getTool(EditFileTool.TOOL_NAME);
    }

    ensureRealm(() -> {
      unbindWorkingSetBar(chatView);
      SessionFileState state = new SessionFileState(chatView);
      state.filesObservable = new WritableValue<>(new LinkedHashMap<>(), Map.class);
      state.buttonEnableObservable = new WritableValue<>(false, Boolean.class);
      sessionStates.put(chatView.getSessionId(), state);
      state.filesSideEffect = ISideEffect.create(() -> state.filesObservable.getValue(),
          (Map<ChangedFile, FileChangeProperty> filesMap) -> {
            if (filesMap.isEmpty()) {
              disposeWorkingSetBar(state);
            } else {
              if (state.workingSetBar == null || state.workingSetBar.isDisposed()) {
                state.workingSetBar = new WorkingSetBar(chatView.getActionBar().getInputArea(), SWT.NONE);
              }
              // Position WorkingSetBar below TodoListBar (if present), otherwise at the top of
              // inputArea. The StaticBanner sits on the outer ActionBar as a sibling of inputArea,
              // so it remains above this bar regardless of this call.
              positionWorkingSetBar(state);
              state.workingSetBar.buildSummaryBarFor(filesMap);
            }
          });
      state.buttonEnableSideEffect = ISideEffect.create(() -> state.buttonEnableObservable.getValue(),
          (Boolean status) -> {
        if (state.workingSetBar == null || state.workingSetBar.isDisposed()) {
          return;
        }
        state.workingSetBar.setButtonStatus(status);
      });
    });
  }

  /**
   * Unbind the WorkingSetBar and dispose side effects.
   */
  public void unbindWorkingSetBar() {
    for (SessionFileState state : new ArrayList<>(sessionStates.values())) {
      unbindWorkingSetBar(state.chatView);
    }
  }

  public void unbindWorkingSetBar(ChatView chatView) {
    SessionFileState state = chatView != null ? sessionStates.remove(chatView.getSessionId()) : null;
    if (state == null) {
      return;
    }
    ensureRealm(() -> {
      if (state.filesSideEffect != null) {
        state.filesSideEffect.dispose();
      }
      if (state.buttonEnableSideEffect != null) {
        state.buttonEnableSideEffect.dispose();
      }
      disposeWorkingSetBar(state);
    });
  }

  /**
   * Position the WorkingSetBar below TodoListBar if present, otherwise at top.
   */
  private void positionWorkingSetBar(SessionFileState state) {
    if (state.workingSetBar == null || state.workingSetBar.isDisposed()) {
      return;
    }
    TodoListService todoListService = CopilotUi.getPlugin().getChatServiceManager().getTodoListService();
    if (todoListService != null && todoListService.getTodoListBar() != null
        && !todoListService.getTodoListBar().isDisposed()) {
      // Position below TodoListBar
      state.workingSetBar.moveBelow(todoListService.getTodoListBar());
    } else {
      // No TodoListBar, position at top
      state.workingSetBar.moveAbove(null);
    }
  }

  /**
   * Enable or disable the buttons for the working set bar.
   */
  public void setWorkingSetBarButtonStatus(boolean status) {
    SessionFileState state = currentState();
    if (state == null) {
      return;
    }
    ensureRealm(() -> {
      state.buttonEnableObservable.setValue(status);
    });
  }

  /**
   * Set the changed files for the working set bar.
   */
  public void setChangedFiles(Map<ChangedFile, FileChangeProperty> files) {
    SessionFileState state = currentState();
    if (state == null) {
      return;
    }
    ensureRealm(() -> {
      state.filesObservable.setValue(files);
    });
  }

  /**
   * Get the changed files for the working set bar.
   */
  public Map<ChangedFile, FileChangeProperty> getChangedFiles() {
    SessionFileState state = currentState();
    return state != null ? state.filesObservable.getValue() : Map.of();
  }

  /**
   * Get the WorkingSetBar instance.
   */
  public WorkingSetBar getWorkingSetBar() {
    SessionFileState state = currentState();
    return state != null ? state.workingSetBar : null;
  }

  /**
   * Add a changed file to the working set bar.
   */
  public void addChangedFile(ChangedFile file, FileChangeType fileChangeType) {
    SessionFileState state = currentState();
    if (state == null) {
      return;
    }
    ensureRealm(() -> {
      Map<ChangedFile, FileChangeProperty> filesMap = new LinkedHashMap<>(state.filesObservable.getValue());
      if (filesMap.containsKey(file)) {
        return;
      }
      filesMap.put(file, new FileChangeProperty(fileChangeType));
      state.filesObservable.setValue(filesMap);
      state.buttonEnableObservable.setValue(false);
    });
  }

  /**
   * Complete a changed file action and remove it from the working set bar.
   *
   * @param file the file to complete
   */
  public void completeFile(ChangedFile file) {
    SessionFileState state = currentState();
    if (state == null) {
      return;
    }
    ensureRealm(() -> {
      Map<ChangedFile, FileChangeProperty> filesMap = new LinkedHashMap<>(state.filesObservable.getValue());
      filesMap.remove(file);
      state.filesObservable.setValue(filesMap);

      if (filesMap.isEmpty()) {
        onResolveAllChanges(state);
      }
    });
  }

  /**
   * Get the file change type of a file.
   *
   * @param file the file to get the change type for
   * @return the file change type, or null if the file is not in the list
   */
  private FileChangeType getFileChangeTypeInternal(ChangedFile file) {
    SessionFileState state = currentState();
    FileChangeProperty property = state != null ? state.filesObservable.getValue().get(file) : null;
    if (property != null) {
      return property.getChangeType();
    } else {
      return null;
    }
  }

  /**
   * Handles the action of keeping changes to a file.
   *
   * @param file the file to keep changes for
   */
  public void onKeepChange(ChangedFile file) {
    if (getFileChangeTypeInternal(file) == FileChangeType.Created) {
      this.createFileTool.onKeepChange(file);
    } else if (getFileChangeTypeInternal(file) == FileChangeType.Changed) {
      this.editFileTool.onKeepChange(file);
    }
    this.completeFile(file);
  }

  /**
   * Handles the action of keeping all changes to files.
   */
  public void onKeepAllChanges() {
    SessionFileState state = currentState();
    if (state == null) {
      return;
    }
    for (ChangedFile file : new ArrayList<>(state.filesObservable.getValue().keySet())) {
      if (getFileChangeTypeInternal(file) == FileChangeType.Created) {
        this.createFileTool.onKeepChange(file);
      } else if (getFileChangeTypeInternal(file) == FileChangeType.Changed) {
        this.editFileTool.onKeepChange(file);
      }
    }
    onResolveAllChanges();
  }

  /**
   * Handles the action of undoing changes to a file.
   *
   * @param file the file to undo changes for
   */
  public void onUndoChange(ChangedFile file) {
    try {
      if (getFileChangeTypeInternal(file) == FileChangeType.Created) {
        this.createFileTool.onUndoChange(file);
      } else if (getFileChangeTypeInternal(file) == FileChangeType.Changed) {
        this.editFileTool.onUndoChange(file);
      }
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Error undoing changes for the new file", e);
    }
    this.completeFile(file);
  }

  /**
   * Handles the action of undoing all changes to files.
   */
  public void onUndoAllChanges() {
    SessionFileState state = currentState();
    if (state == null) {
      return;
    }
    try {
      for (ChangedFile file : new ArrayList<>(state.filesObservable.getValue().keySet())) {
        if (getFileChangeTypeInternal(file) == FileChangeType.Created) {
          this.createFileTool.onUndoChange(file);
        } else if (getFileChangeTypeInternal(file) == FileChangeType.Changed) {
          this.editFileTool.onUndoChange(file);
        }
      }
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Error undoing all changes for the files", e);
    }
    onResolveAllChanges();
  }

  /**
   * Handles the action of viewing the diff of a file.
   *
   * @param file the file to view the diff for
   */
  public void onViewDiff(ChangedFile file) {
    SessionFileState state = currentState();
    FileChangeProperty property = state != null ? state.filesObservable.getValue().get(file) : null;
    if (property == null) {
      return;
    }
    if (property.getChangeType() == FileChangeType.Created) {
      this.createFileTool.onViewDiff(file);
    } else if (property.getChangeType() == FileChangeType.Changed) {
      this.editFileTool.onViewDiff(file);
    }
  }

  /**
   * Handles the action of clicking done button to resolve all changes.
   */
  public void onResolveAllChanges() {
    SessionFileState state = currentState();
    if (state != null) {
      onResolveAllChanges(state);
    }
  }

  private void onResolveAllChanges(SessionFileState state) {
    this.createFileTool.onResolveAllChanges();
    this.editFileTool.onResolveAllChanges();
    ensureRealm(() -> {
      state.filesObservable.setValue(new LinkedHashMap<>());
      state.buttonEnableObservable.setValue(false);
      disposeWorkingSetBar(state);
    });
  }

  /**
   * Dispose the WorkingSetBar.
   */
  public void disposeWorkingSetBar() {
    SessionFileState state = currentState();
    if (state != null) {
      disposeWorkingSetBar(state);
    }
  }

  public void disposeWorkingSetBar(String sessionId) {
    SessionFileState state = sessionStates.get(sessionId);
    if (state != null) {
      disposeWorkingSetBar(state);
    }
  }

  private void disposeWorkingSetBar(SessionFileState state) {
    if (state.workingSetBar != null && !state.workingSetBar.isDisposed()) {
      Composite control = state.workingSetBar.getParent();
      state.workingSetBar.dispose();
      state.workingSetBar = null;
      control.requestLayout();
    }
  }

  public void enterInvocationSession(String sessionId) {
    invocationSession.set(sessionId);
  }

  public void exitInvocationSession() {
    invocationSession.remove();
  }

  private SessionFileState currentState() {
    String sessionId = invocationSession.get();
    if (sessionId == null) {
      CopilotSession active = CopilotUi.getPlugin().getChatServiceManager().getSessionRegistry().getActive();
      sessionId = active != null ? active.getSessionId() : null;
    }
    return sessionId != null ? sessionStates.get(sessionId) : null;
  }

  private static final class SessionFileState {
    private final ChatView chatView;
    private IObservableValue<Map<ChangedFile, FileChangeProperty>> filesObservable;
    private IObservableValue<Boolean> buttonEnableObservable;
    private WorkingSetBar workingSetBar;
    private ISideEffect filesSideEffect;
    private ISideEffect buttonEnableSideEffect;

    private SessionFileState(ChatView chatView) {
      this.chatView = chatView;
    }
  }

  /**
   * Class for file change properties.
   */
  public static class FileChangeProperty {
    private FileChangeType changeType;

    /**
     * Constructor for FileChangeProperty.
     *
     * @param changeType The type of file change (new or edited).
     */
    public FileChangeProperty(FileChangeType changeType) {
      this.changeType = changeType;
    }

    public FileChangeType getChangeType() {
      return changeType;
    }
  }
}
