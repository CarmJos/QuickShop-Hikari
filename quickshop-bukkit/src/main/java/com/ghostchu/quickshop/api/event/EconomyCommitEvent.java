/*
 *  This file is a part of project QuickShop, the name is EconomyCommitEvent.java
 *  Copyright (C) Ghost_chu and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ghostchu.quickshop.api.event;

import com.ghostchu.quickshop.api.economy.EconomyTransaction;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calling when transaction will commit
 */
public class EconomyCommitEvent extends AbstractQSEvent implements QSCancellable {
    private final EconomyTransaction transaction;
    private boolean cancelled;
    private @Nullable Component cancelReason;

    /**
     * Calling when transaction will commit
     *
     * @param transaction transaction
     */
    public EconomyCommitEvent(@NotNull EconomyTransaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Gets the transaction in this event
     *
     * @return transaction
     */
    public EconomyTransaction getTransaction() {
        return transaction;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }


    @Override
    public void setCancelled(boolean cancel, @Nullable Component reason) {
        this.cancelled = cancel;
        this.cancelReason = reason;
    }

    @Override
    public @Nullable Component getCancelReason() {
        return this.cancelReason;
    }
}
