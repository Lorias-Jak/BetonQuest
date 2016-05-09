/**
 * BetonQuest - advanced quests for Bukkit
 * Copyright (C) 2015  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.compatibility.mcmmo;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.api.SkillAPI;

import pl.betoncraft.betonquest.InstructionParseException;
import pl.betoncraft.betonquest.VariableNumber;
import pl.betoncraft.betonquest.api.Condition;
import pl.betoncraft.betonquest.utils.PlayerConverter;

/**
 * Checks if the player has specified level in an mcMMO skill.
 * 
 * @author Jakub Sapalski
 */
public class McMMOSkillLevelCondition extends Condition {

	private final String skillType;
	private final VariableNumber level;

	public McMMOSkillLevelCondition(String packName, String instructions) throws InstructionParseException {
		super(packName, instructions);
		String[] parts = instructions.split(" ");
		if (parts.length < 3) {
			throw new InstructionParseException("Not enough arguments");
		}
		skillType = parts[1].toUpperCase();
		if (!SkillAPI.getSkills().contains(skillType)) {
			throw new InstructionParseException("Invalid skill name");
		}
		try {
			VariableNumber tempLevel = new VariableNumber(packName, parts[2]);
			level = tempLevel;
		} catch (NumberFormatException e) {
			throw new InstructionParseException("Could not parse level");
		}
	}

	@Override
	public boolean check(String playerID) {
		return ExperienceAPI.getLevel(PlayerConverter.getPlayer(playerID), skillType) >= level.getInt(playerID);
	}

}
