/*
 * Copyright © 2017 xujun and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package vtn_access_controller.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vtn_access_controller.cli.api.Vtn_access_controllerCliCommands;

public class Vtn_access_controllerCliCommandsImpl implements Vtn_access_controllerCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(Vtn_access_controllerCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public Vtn_access_controllerCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("Vtn_access_controllerCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}