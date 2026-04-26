package me.danshi.dCore.menu;

import com.cryptomorin.xseries.XSound;
import me.danshi.dCore.DCore;
import org.bukkit.entity.Player;

public sealed interface Action permits Action.Cmd, Action.Msg, Action.Snd, Action.Close, Action.EcoGive, Action.EcoTake, Action.NextPage, Action.PrevPage {
    void execute(Player p);

    record Cmd(boolean console, String command) implements Action {
        @Override
        public void execute(Player p) {
            var cmd = command.replace("%player%", p.getName());
            if (console) p.getServer().dispatchCommand(p.getServer().getConsoleSender(), cmd);
            else p.performCommand(cmd);
        }
    }

    record Msg(String text) implements Action {
        @Override
        public void execute(Player p) {
            p.sendMessage(DCore.get().miniMessage().deserialize(text));
        }
    }

    record Snd(String sound, float vol, float pitch) implements Action {
        @Override
        public void execute(Player p) {
            XSound.matchXSound(sound).ifPresent(s -> s.play(p, vol, pitch));
        }
    }

    record Close() implements Action {
        @Override
        public void execute(Player p) { p.closeInventory(); }
    }

    record EcoGive(String currency, double amount) implements Action {
        @Override
        public void execute(Player p) {
            var db = DCore.get().getDbManager();
            String uuid = p.getUniqueId().toString();

            // Manejo asíncrono: cuando llegue el resultado, sumamos y guardamos
            db.getBalance(uuid, currency).thenAccept(current -> {
                db.setBalance(uuid, currency, current + amount);
            });
        }
    }

    record EcoTake(String currency, double amount) implements Action {
        @Override
        public void execute(Player p) {
            var db = DCore.get().getDbManager();
            String uuid = p.getUniqueId().toString();

            // Manejo asíncrono: cuando llegue el resultado, restamos y guardamos
            db.getBalance(uuid, currency).thenAccept(current -> {
                db.setBalance(uuid, currency, current - amount);
            });
        }
    }

    record NextPage() implements Action {
        @Override
        public void execute(Player p) { MenuRegistry.nextPage(p); }
    }

    record PrevPage() implements Action {
        @Override
        public void execute(Player p) { MenuRegistry.prevPage(p); }
    }
}
