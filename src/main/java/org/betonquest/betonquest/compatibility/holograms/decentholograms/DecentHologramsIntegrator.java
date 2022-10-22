package org.betonquest.betonquest.compatibility.holograms.decentholograms;

import lombok.CustomLog;
import org.betonquest.betonquest.api.config.QuestPackage;
import org.betonquest.betonquest.compatibility.holograms.HologramIntegrator;
import org.betonquest.betonquest.compatibility.holograms.HologramSubIntegrator;
import org.betonquest.betonquest.exceptions.HookException;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;

@CustomLog
public class DecentHologramsIntegrator extends HologramSubIntegrator {

    public DecentHologramsIntegrator() {
        super("DecentHolograms", DecentHologramsHologram.class, "2.7.3");
    }

    @Override
    public void init() throws HookException {
        super.init(); //Calling super validates version
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            LOG.warn("Holograms from DecentHolograms will not be able to use BetonQuest variables in text-lines" +
                    "without PlaceholderAPI plugin! Install it to use holograms with variables!");
        }
    }

    @Override
    public String parseInstructionVariable(final QuestPackage pack, final String text) {
        /* We must convert a normal BetonQuest variable such as "%objective.kills.left% to
        %betonquest_pack:objective.kills.left% which is parsed by DecentHolograms as a PlaceholderAPI placeholder. */
        final Matcher matcher = HologramIntegrator.INSTRUCTION_VARIABLE_VALIDATOR.matcher(text);
        return matcher.replaceAll(match -> "%betonquest_" + pack.getPackagePath() + ":" + match.group().replaceAll("%", ""));
    }
}
