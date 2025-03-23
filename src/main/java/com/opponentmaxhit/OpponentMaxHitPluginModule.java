package com.opponentmaxhit;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class OpponentMaxHitPluginModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(WikiService.class).in(Singleton.class);
    }
}