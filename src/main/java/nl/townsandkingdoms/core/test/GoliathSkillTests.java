package nl.townsandkingdoms.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.townsandkingdoms.core.boss.GoliathSkillLibrary;

/** Runs without optional TNK3 mods and validates all converted source skills. */
@GameTestHolder("tnkcore")
@PrefixGameTestTemplate(false)
public final class GoliathSkillTests {
    @GameTest(template = "empty")
    public static void originalDefinitionsAndEventsPresent(GameTestHelper helper) {
        var library = GoliathSkillLibrary.load(helper.getLevel());
        helper.assertTrue(library != null, "Full Sculk Goliath data is missing from the jar");
        helper.assertTrue(library.definitionCount() == 115,
                "Original 115 named definitions were not fully converted");
        helper.assertTrue(library.mechanicCount() == 1370,
                "One or more source action lines were lost");
        helper.assertTrue(library.eventTriggerCount() == 48,
                "Some original phase events were not converted");
        helper.assertTrue(library.unresolvedSkillReferences() == 0,
                "One or more nested skills or phase triggers reference a missing definition");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void repeatedAndTargetedMechanicsParse(GameTestHelper helper) {
        var source = "throw{v=8;vy=2;fo=true;repeat=4;repeati=8;delay=15} @PIR{r=24}";
        var action = GoliathSkillLibrary.parseStep(source);
        helper.assertTrue(action.operation().equals("throw"), "Wrong mechanic verb");
        helper.assertTrue(action.target().equals("pir") && action.targetArgs().get("r").equals("24"),
                "The area-of-effect target was not preserved");
        helper.assertTrue(action.delayTicks() == 15 && action.repeats() == 4 &&
                action.repeatInterval() == 8, "Scheduling timings were not preserved");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void projectileCallbacksInheritTargetsAndResolveHealingAlias(GameTestHelper helper) {
        var damage = GoliathSkillLibrary.parseStep("damage{a=3}");
        helper.assertTrue(damage.target().equals("inherited"), "Hit damage must inherit its projectile target");
        helper.assertTrue(GoliathSkillLibrary.parseStep("heal{a=3} @parent").target().equals("parent"),
                "Explicit healing parent must remain the boss");
        var library = GoliathSkillLibrary.load(helper.getLevel());
        helper.assertTrue(library != null && library.definition("SG_S_SHOOT_OH_SELF") ==
                library.definition("SG_S_SHOOT_OHB_SELF"), "Missing source callback must resolve to the healing skill");
        helper.assertTrue(library.definitionCount() == 115 && library.mechanicCount() == 1370,
                "Native callback repair must preserve original source definitions");
        helper.succeed();
    }
}
