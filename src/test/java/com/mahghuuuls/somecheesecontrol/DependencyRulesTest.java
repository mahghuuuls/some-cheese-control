package com.mahghuuuls.somecheesecontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the package dependency rules of the architecture by scanning the imports of
 * {@code src/main/java}. Fully qualified names written in code, without an import, are not seen.
 */
class DependencyRulesTest {

    private static final String ROOT = "com.mahghuuuls.somecheesecontrol";
    private static final String CLIMBING_TRACKER = ROOT + ".feature.climbing.ClimbingTracker";
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)", Pattern.MULTILINE);
    /** The one other mod each integration package may import, besides MixinBooter; a new integration adds its own. */
    private static final Map<String, String> INTEGRATION_MOD_PACKAGES = new HashMap<>();

    static {
        INTEGRATION_MOD_PACKAGES.put("comforts", "c4.comforts.");
        INTEGRATION_MOD_PACKAGES.put("wizardry", "electroblob.wizardry.");
        INTEGRATION_MOD_PACKAGES.put("spartanshields", "com.oblivioussp.");
    }

    /** Packages every part of the mod may use; anything else belongs to another mod. */
    private static final List<String> PLATFORM_PACKAGES = Arrays.asList(
            "java.", "javax.", "net.minecraft.", "net.minecraftforge.", "org.apache.logging.log4j.",
            "com.google.", "io.netty.", ROOT + ".");

    @Test
    void sourcesFollowTheDependencyRules() throws IOException {
        Map<String, List<String>> imports = readImports(Paths.get("src", "main", "java"));
        assertTrue(imports.containsKey(ROOT + ".SomeCheeseControlMod"), "the scan read the mod's sources");
        assertEquals(Collections.emptyList(), violations(imports));
    }

    @Test
    void scanFindsAForbiddenImportInASourceFile(@TempDir Path root) throws IOException {
        Path dir = Files.createDirectories(root.resolve("com/mahghuuuls/somecheesecontrol/feature/liquid"));
        Files.write(dir.resolve("Bad.java"), Arrays.asList(
                "package com.mahghuuuls.somecheesecontrol.feature.liquid;",
                "",
                "import com.mahghuuuls.somecheesecontrol.feature.carry.CarryFeature;",
                "import net.minecraft.client.Minecraft;",
                "",
                "class Bad {}"), StandardCharsets.UTF_8);
        List<String> found = violations(readImports(root));
        assertEquals(2, found.size(), found.toString());
    }

    @Test
    void rulesDetectForbiddenImports() {
        Map<String, List<String>> imports = new HashMap<>();
        imports.put(ROOT + ".feature.liquid.A", Arrays.asList(ROOT + ".feature.carry.B"));
        imports.put(ROOT + ".core.C", Arrays.asList(ROOT + ".feature.liquid.A"));
        imports.put(ROOT + ".config.D", Arrays.asList(ROOT + ".core.Rule"));
        imports.put(ROOT + ".feature.bed.E", Arrays.asList("c4.comforts.common.blocks.BlockBase"));
        imports.put(ROOT + ".feature.climbing.F", Arrays.asList("bettercombat.mod.util.Helpers"));
        imports.put(ROOT + ".integration.wizardry.G", Arrays.asList("net.minecraft.client.Minecraft"));
        imports.put(ROOT + ".feature.cobweb.H", Arrays.asList(ROOT + ".diagnostic.StatusCommand"));
        imports.put(ROOT + ".mixin.spartanshields.I", Arrays.asList(ROOT + ".feature.bed.E"));
        imports.put(ROOT + ".network.J", Arrays.asList(ROOT + ".core.Refusals"));
        imports.put(ROOT + ".core.K", Arrays.asList(ROOT + ".client.EarlyClientRefusal"));
        imports.put(ROOT + ".feature.liquid.L", Arrays.asList(ROOT + ".network.Network"));
        imports.put(ROOT + ".client.M", Arrays.asList(ROOT + ".diagnostic.SccCommand"));
        imports.put(ROOT + ".network.N", Arrays.asList("net.minecraft.client.Minecraft"));
        imports.put(ROOT + ".feature.carry.Q", Arrays.asList("org.lwjgl.input.Mouse"));
        imports.put(ROOT + ".client.O", Arrays.asList(ROOT + ".integration.wizardry.WizardryBridge"));
        imports.put(ROOT + ".client.P", Arrays.asList(ROOT + ".feature.liquid.LiquidPlacementFeature"));
        imports.put(ROOT + ".mixin.client.vanilla.R", Arrays.asList(ROOT + ".core.Refusals"));
        List<String> found = violations(imports);
        assertEquals(17, found.size(), found.toString());
    }

    @Test
    void allowedImportsPass() {
        Map<String, List<String>> imports = new HashMap<>();
        imports.put(ROOT + ".feature.liquid.A", Arrays.asList(CLIMBING_TRACKER, CLIMBING_TRACKER + ".State", ROOT + ".core.Refusals"));
        imports.put(ROOT + ".integration.wizardry.F", Arrays.asList("electroblob.wizardry.event.SpellCastEvent", CLIMBING_TRACKER));
        imports.put(ROOT + ".mixin.spartanshields.M", Arrays.asList("org.spongepowered.asm.mixin.Mixin", ROOT + ".integration.spartanshields.Bridge"));
        imports.put(ROOT + ".mixin.SccLateMixins", Arrays.asList("zone.rong.mixinbooter.ILateMixinLoader", "net.minecraftforge.fml.common.Loader"));
        imports.put(ROOT + ".SomeCheeseControlMod", Arrays.asList(ROOT + ".diagnostic.SccCommand", ROOT + ".feature.liquid.A",
                ROOT + ".client.EarlyClientRefusal", ROOT + ".network.Network"));
        imports.put(ROOT + ".client.C", Arrays.asList("net.minecraft.client.Minecraft", "org.lwjgl.input.Mouse", ROOT + ".feature.climbing.ClimbingRules",
                ROOT + ".feature.liquid.LiquidPlacement", ROOT + ".feature.carry.CarryRules", ROOT + ".core.Rule", ROOT + ".config.Settings", ROOT + ".network.N"));
        imports.put(ROOT + ".network.N", Arrays.asList(ROOT + ".config.Settings", "io.netty.buffer.ByteBuf"));
        imports.put(ROOT + ".mixin.client.S", Arrays.asList("net.minecraft.client.Minecraft", ROOT + ".client.C",
                "com.llamalad7.mixinextras.injector.v2.WrapWithCondition", "org.objectweb.asm.tree.ClassNode"));
        assertEquals(Collections.emptyList(), violations(imports));
    }

    @Test
    void anIntegrationImportsOnlyItsOwnMod() {
        assertEquals(null, check(ROOT + ".integration.comforts.G", "c4.comforts.common.capability.CapabilitySleeping"));
        assertEquals("an integration imports only its own mod",
                check(ROOT + ".integration.wizardry.W", "c4.comforts.common.capability.CapabilitySleeping"));
        assertEquals(null, check(ROOT + ".integration.spartanshields.S", "zone.rong.mixinbooter.ILateMixinLoader"),
                "an integration may use MixinBooter");
    }

    @Test
    void theGamesInputLibraryIsClientOnly() {
        assertEquals("client class outside client and mixin.client", check(ROOT + ".feature.carry.Q", "org.lwjgl.input.Mouse"));
        assertEquals(null, check(ROOT + ".client.Q", "org.lwjgl.input.Mouse"));
    }

    static List<String> violations(Map<String, List<String>> importsByClass) {
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : importsByClass.entrySet()) {
            String from = entry.getKey();
            for (String target : entry.getValue()) {
                String problem = check(from, target);
                if (problem != null) {
                    found.add(from + " -> " + target + ": " + problem);
                }
            }
        }
        Collections.sort(found);
        return found;
    }

    static String check(String from, String target) {
        String fromPkg = relativePackage(from);
        if (target.startsWith("net.minecraft.client.") || target.startsWith("org.lwjgl.")) {
            return isClientCode(fromPkg) ? null : "client class outside client and mixin.client";
        }
        if (!isPlatform(target)) {
            if (fromPkg.startsWith("integration.")) {
                String own = INTEGRATION_MOD_PACKAGES.get(segment(fromPkg, 1));
                boolean mixinBooter = target.startsWith("zone.rong.mixinbooter.") || target.startsWith("org.spongepowered.");
                return mixinBooter || own != null && target.startsWith(own) ? null : "an integration imports only its own mod";
            }
            return mayImportOtherMods(fromPkg) ? null : "another mod's class outside integration or mixin";
        }
        if (!target.startsWith(ROOT + ".")) {
            return null;
        }
        String targetPkg = relativePackage(target);
        if (targetPkg.startsWith("diagnostic") && !fromPkg.isEmpty() && !fromPkg.startsWith("diagnostic")) {
            return "only the main class may import diagnostic";
        }
        if (inPackage(targetPkg, "client") && !(fromPkg.isEmpty() || isClientCode(fromPkg))) {
            return "only the main class and mixin.client import client";
        }
        if (inPackage(fromPkg, "network") && !(inPackage(targetPkg, "network") || inPackage(targetPkg, "config"))) {
            return "network depends only on config";
        }
        if (inPackage(fromPkg, "client") && !(inPackage(targetPkg, "client") || targetPkg.startsWith("feature.")
                || inPackage(targetPkg, "core") || inPackage(targetPkg, "config") || inPackage(targetPkg, "network"))) {
            return "client depends only on features, core, config and network";
        }
        if (inPackage(fromPkg, "client") && targetPkg.startsWith("feature.") && target.endsWith("Feature")) {
            return "client uses feature rule classes and queries, not their event handlers";
        }
        if (fromPkg.startsWith("config") && !targetPkg.startsWith("config")) {
            return "config depends on nothing else";
        }
        if (fromPkg.startsWith("core") && !(targetPkg.startsWith("core") || targetPkg.startsWith("config"))) {
            return "core depends only on config";
        }
        if (fromPkg.startsWith("feature.")) {
            if (targetPkg.startsWith("integration") || targetPkg.startsWith("mixin")) {
                return "features do not import integrations";
            }
            if (inPackage(targetPkg, "network") || inPackage(targetPkg, "client")) {
                return "features do not import network or client";
            }
            boolean otherFeature = targetPkg.startsWith("feature.") && !segment(targetPkg, 1).equals(segment(fromPkg, 1));
            if (otherFeature && !(target.equals(CLIMBING_TRACKER) || target.startsWith(CLIMBING_TRACKER + "."))) {
                return "features do not import other features";
            }
        }
        if (inPackage(fromPkg, "mixin.client") && target.startsWith(ROOT + ".")
                && !(inPackage(targetPkg, "client") || inPackage(targetPkg, "mixin.client"))) {
            return "mixin.client calls only client";
        }
        if (fromPkg.startsWith("mixin.spartanshields")
                && !(targetPkg.startsWith("mixin") || targetPkg.startsWith("integration.spartanshields"))) {
            return "the Mixin calls only integration.spartanshields";
        }
        return null;
    }

    private static boolean isPlatform(String target) {
        for (String prefix : PLATFORM_PACKAGES) {
            if (target.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** The packages that may reference client-only Minecraft classes. */
    private static boolean isClientCode(String pkg) {
        return inPackage(pkg, "client") || inPackage(pkg, "mixin.client");
    }

    /** True when {@code pkg} is {@code name} or a package under it. */
    private static boolean inPackage(String pkg, String name) {
        return pkg.equals(name) || pkg.startsWith(name + ".");
    }

    private static boolean mayImportOtherMods(String pkg) {
        return pkg.startsWith("integration.") || pkg.startsWith("mixin");
    }

    /** Package of {@code className} relative to the root, without the class name; "" for the root. */
    private static String relativePackage(String className) {
        String rel = className.startsWith(ROOT + ".") ? className.substring(ROOT.length() + 1) : className;
        int dot = rel.lastIndexOf('.');
        return dot < 0 ? "" : rel.substring(0, dot);
    }

    private static String segment(String pkg, int index) {
        String[] parts = pkg.split("\\.");
        return parts.length > index ? parts[index] : "";
    }

    private static Map<String, List<String>> readImports(Path sourceRoot) throws IOException {
        Map<String, List<String>> result = new HashMap<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
                String className = relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                List<String> imports = new ArrayList<>();
                Matcher m = IMPORT.matcher(text);
                while (m.find()) {
                    imports.add(m.group(1));
                }
                result.put(className, imports);
            }
        }
        return result;
    }
}
