package com.dtteam.dynamictreesplus.data;

import com.dtteam.dynamictrees.data.generator.DataGenerators;
import com.dtteam.dynamictreesplus.DynamicTreesPlus;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;

public class DTPDataGenerators {

    public static void register() {
        DataGenerators.registerBlockModelGenerator(CapProperties.class, CapProperties.CAP_GENERATOR, CapStateGenerator::new);
        DataGenerators.registerBlockModelGenerator(CapProperties.class, CapProperties.CAP_CENTER_GENERATOR, CapCenterStateGenerator::new);
    }

}
