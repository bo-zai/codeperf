package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.config.AgentConfig;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstrumentationInstallerTest {

    @Test
    public void should_NotMatchAdjacentPackagePrefix_When_TargetPackageIsShorter() {
        InstrumentationInstaller installer = new InstrumentationInstaller();
        ElementMatcher.Junction<TypeDescription> matcher =
                installer.buildPackageMatcher(Arrays.asList("com.cmb"));

        assertTrue(matcher.matches(TypeDescription.ForLoadedType.of(com.cmb.order.SampleService.class)));
        assertFalse(matcher.matches(TypeDescription.ForLoadedType.of(com.cmbchina.order.SampleService.class)));
    }

    @Test
    public void should_IgnoreDefaultCompanyFrameworkPackages_When_BuildingIgnoreMatcher() {
        InstrumentationInstaller installer = new InstrumentationInstaller();
        AgentConfig config = new AgentConfig();
        ElementMatcher.Junction<TypeDescription> ignoredPackages =
                installer.buildPackageMatcher(config.getExcludedPackages());
        ElementMatcher.Junction<TypeDescription> matcher = installer.buildIgnoreMatcher(ignoredPackages);

        assertTrue(matcher.matches(TypeDescription.ForLoadedType.of(com.cmb.checkerframework.FrameworkModule.class)));
        assertFalse(matcher.matches(TypeDescription.ForLoadedType.of(com.cmb.order.SampleService.class)));
    }

    @Test
    public void should_IgnoreAgentOwnClasses_When_TargetPackageIncludesCompanyRoot() {
        InstrumentationInstaller installer = new InstrumentationInstaller();
        ElementMatcher.Junction<TypeDescription> matcher = installer.buildIgnoreMatcher(null);

        assertTrue(matcher.matches(TypeDescription.ForLoadedType.of(com.cmb.codeperf.agent.AgentBootstrap.class)));
    }
}
