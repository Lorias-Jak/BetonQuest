package org.betonquest.betonquest.api.quest.event;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.quest.PlayerlessQuestFactory;
import org.betonquest.betonquest.exceptions.InstructionParseException;

/**
 * Factory to create a specific {@link StaticEvent} from {@link Instruction}s.
 */
public interface StaticEventFactory extends PlayerlessQuestFactory<StaticEvent> {
    /**
     * Parses an instruction to create a {@link StaticEvent}.
     *
     * @param instruction instruction to parse
     * @return event represented by the instruction
     * @throws InstructionParseException when the instruction cannot be parsed
     */
    StaticEvent parseStaticEvent(Instruction instruction) throws InstructionParseException;

    @Override
    default StaticEvent parsePlayerless(final Instruction instruction) throws InstructionParseException {
        return parseStaticEvent(instruction);
    }
}
