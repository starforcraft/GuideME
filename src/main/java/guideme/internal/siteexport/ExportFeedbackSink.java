package guideme.internal.siteexport;

import net.minecraft.network.chat.Component;

public interface ExportFeedbackSink {
    /**
     * Sends a feedback message to the player.
     *
     * @param message the feedback message
     */
    void sendFeedback(Component message);

    /**
     * Sends an error message to the player.
     *
     * @param message the error message
     */
    void sendError(Component message);
}
