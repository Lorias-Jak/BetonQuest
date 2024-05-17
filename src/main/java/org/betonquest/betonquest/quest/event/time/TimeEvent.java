package org.betonquest.betonquest.quest.event.time;

import org.betonquest.betonquest.VariableNumber;
import org.betonquest.betonquest.api.common.function.Selector;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.api.quest.event.HybridEvent;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * The time event, changing the time on the server.
 */
public class TimeEvent implements HybridEvent {
    /**
     * The type of time that will be applied.
     */
    private final Time time;

    /**
     * The selector to get the world for that the time should be set.
     */
    private final Selector<World> worldSelector;

    /**
     * The raw time value that will be applied.
     */
    private final VariableNumber rawTime;

    /**
     * If the rawTime needs to be transformed into Minecraft format.
     */
    private final boolean hourFormat;

    /**
     * Creates the time event.
     *
     * @param time          the time type to set
     * @param rawTime       the raw time value to set
     * @param worldSelector to get the world that should be affected
     * @param hourFormat    if the time needs to be multiplied with 1000
     */
    public TimeEvent(final Time time, final VariableNumber rawTime, final Selector<World> worldSelector, final boolean hourFormat) {
        this.time = time;
        this.rawTime = rawTime;
        this.worldSelector = worldSelector;
        this.hourFormat = hourFormat;
    }

    @Override
    public void execute(@Nullable final Profile profile) throws QuestRuntimeException {
        final World world = worldSelector.selectFor(profile);
        final double timeValue = rawTime.getDouble(profile);
        final long actualTime = (long) Math.abs(hourFormat ? timeValue * 1000 : timeValue);
        world.setTime(time.applyTo(world, actualTime));
    }
}
