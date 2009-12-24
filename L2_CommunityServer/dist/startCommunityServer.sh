#!/bin/sh
java -Xms128m -Xmx128m -cp ./../libs/*:l2jcommunity.jar com.l2jserver.communityserver.L2CommunityServer > log/stdout.log 2>&1