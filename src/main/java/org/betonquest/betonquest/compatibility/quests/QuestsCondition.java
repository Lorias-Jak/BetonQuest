package org.betonquest.betonquest.compatibility.quests;

import me.pikamug.quests.quests.Quest;
import org.betonquest.betonquest.api.Condition;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.QuestException;
import org.betonquest.betonquest.instruction.Instruction;

import java.util.Set;

/**
 * Checks if the player has done specified quest before.
 */
@SuppressWarnings("PMD.CommentRequired")
public class QuestsCondition extends Condition {

    private final String questName;

    public QuestsCondition(final Instruction instruction) throws QuestException {
        super(instruction, true);
        questName = instruction.next();
    }

    @Override
    protected Boolean execute(final Profile profile) {
        final Set<Quest> completedQuests = QuestsIntegrator.getQuestsInstance().getQuester(profile.getProfileUUID()).getCompletedQuests();
        for (final Quest q : completedQuests) {
            if (q.getName().replace(' ', '_').equalsIgnoreCase(questName)) {
                return true;
            }
        }
        return false;
    }
}
