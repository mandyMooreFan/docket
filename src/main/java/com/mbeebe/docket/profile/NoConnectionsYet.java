package com.mbeebe.docket.profile;

import org.springframework.stereotype.Component;

/** Until Build Connections (#32) lands, nobody is connected to anybody. */
@Component
class NoConnectionsYet implements ConnectionLookup {

    @Override
    public boolean connected(long memberA, long memberB) {
        return false;
    }
}
