// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.ui.chat.ChatView;

class CopilotSessionRegistryTest {
  @Test
  void keepsOpenViewsAndConversationAliasesIndependent() {
    CopilotSessionRegistry registry = new CopilotSessionRegistry();
    ChatView firstView = mock(ChatView.class);
    ChatView secondView = mock(ChatView.class);
    registry.registerView("first", firstView);
    registry.registerView("second", secondView);
    registry.updateConversation("first", "conversation-1", "subagent-1");
    registry.updateConversation("second", "conversation-2", null);

    assertEquals(2, registry.getOpenSessions().size());
    assertSame(firstView, registry.findByConversation("conversation-1").getView());
    assertSame(firstView, registry.findByConversation("subagent-1").getView());
    assertSame(secondView, registry.findByConversation("conversation-2").getView());
  }

  @Test
  void activeViewAndClosedIdleSessionLifecycle() {
    CopilotSessionRegistry registry = new CopilotSessionRegistry();
    ChatView view = mock(ChatView.class);
    registry.registerView("session", view);
    registry.setActive("session");
    assertSame(view, registry.getActiveView());

    registry.unregisterView("session", view);

    assertNull(registry.getActiveView());
    assertEquals(0, registry.getOpenSessions().size());
  }
}
