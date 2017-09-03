package org.fcrepo.IT.auth;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ModeShapeEngine;

@RunWith(Suite.class)
@Suite.SuiteClasses({ModeShapeHonorsFADResponseIT.class, ContainerRolesPrincipalProviderIT.class,
        DelegatedUserIT.class})
public class ModeShapePersistenceTestSuite {

    private static ModeShapeEngine engine;

    @BeforeClass
    public static void setUp() {
        FileUtil.delete("target/fedora_repository/store/modeshape.repository");
        System.out.println("setting up");

    }
}
