package jn.rocbot.commands.normalcommands.withsubcommand;


import jn.rocbot.Main;
import jn.rocbot.commands.common.CommandConfig;
import jn.rocbot.commands.common.CommandType;
import jn.rocbot.commands.common.SubCommand;
import jn.rocbot.info.ListAble;
import jn.rocbot.info.stores.AuraStore;
import jn.rocbot.info.stores.WeaponStore;
import jn.rocbot.info.stores.ZenStore;
import jn.rocbot.ships.DamageType;
import jn.rocbot.ships.Rarity;
import jn.rocbot.ships.Ship;
import jn.rocbot.utils.Log;
import jn.rocbot.utils.Search;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WithSubCommand implements SubCommand {
    private CALLER caller;

    public WithSubCommand(CALLER caller) {
        this.caller = caller;
    }

    @Override
    public String invoke() {
        return "with";
    }

    public enum CALLER {
        Ships, Auras
    }

    public WithFilter parseFilter(String[] args){
        return parseFilter(args, this.caller);
    }

    public WithFilter parseFilter(String[] args, CALLER caller){
        switch (caller) {
            case Ships:
                return getShipsFilter(args);
            case Auras:
            default:
                try {
                    throw new OperationNotSupportedException(String.format("the with command does not support %s yet", caller.name()));
                } catch (OperationNotSupportedException e) {
                    e.printStackTrace();
                }
        } return null; //Hopefully it never gets here
    }

    private  <T> T[] concatenate (T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);

        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    private static Logger log = Logger.getLogger(Log.class.getName());

    private void dLog(String msg){
        if(Main.DEBUG) log.log(Level.INFO, msg);
    }

    private WithFilter getShipsFilter(String[] passed_args){
        ArrayList<WithProperty> properties = new ArrayList<>();
        StringJoiner passed = new StringJoiner(" ");
        for (String arg : passed_args) passed.add(arg);

        dLog("Filtering ships, received" +
                "\n\targs: " + Arrays.toString(passed_args) +
                "\n\tJoined: " + passed.toString() +
                "\n\tType: " + getShipPropertyType(passed.toString()).name());

        switch (getShipPropertyType(passed.toString())) {
            case Aura:
                properties.add(
                        new WithProperty(
                                WithProperty.PROPERTY_TYPE.HasShipProperty,
                                "aura:" + passed.toString().toLowerCase()
                        )
                ); break;
            case Zen:
                properties.add(
                        new WithProperty(WithProperty.PROPERTY_TYPE.HasShipProperty,
                                "zen:" + passed.toString().toLowerCase()
                        )
                ); break;
            case Weapon:
                properties.add(
                        new WithProperty(WithProperty.PROPERTY_TYPE.HasShipProperty,
                                "weapon:" +passed.toString().toLowerCase()
                        )
                ); break;
            case Rarity:
                properties.add(
                        new WithProperty(
                                WithProperty.PROPERTY_TYPE.HasShipProperty,
                                "rarity:" + Rarity.fromString(passed.toString().toLowerCase()).name.toLowerCase()
                        )
                ); break;
            case DamageType:
                try {
                    String damageType = "";
                    switch(passed.toString().toLowerCase()) {
                        case "sb": damageType = "sb"; break;
                        case "ap": damageType = "ap"; break;
                        case "hi": damageType = "hi"; break;
                        default: damageType =
                                DamageType.fromString(passed.toString().replace("-", " ")).toString();
                    }

                    properties.add(new WithProperty(
                            WithProperty.PROPERTY_TYPE.HasShipProperty,
                            "damagetype:" + damageType
                            ));
                } catch (Exception e) {
                    e.printStackTrace();
                } break;
            case None:
                break;
        }

        return new WithFilter(properties.toArray(new WithProperty[properties.size()]));
    }

    private Ship.ShipPropertyType getShipPropertyType(String search){
        if(AuraStore.isAura(search))
            return Ship.ShipPropertyType.Aura;

        if(ZenStore.isZen(search))
            return Ship.ShipPropertyType.Zen;

        if(WeaponStore.isWeapon(search))
            return Ship.ShipPropertyType.Weapon;

        if(Rarity.isRarity(search))
            return Ship.ShipPropertyType.Rarity;

        if(DamageType.isDamageType(search.replace("-", " ")))
            return Ship.ShipPropertyType.DamageType;

        return Ship.ShipPropertyType.None; //If none of the above has returned
    }

    public ListAble getWith(WithFilter filter, MessageReceivedEvent event) {
        return getWith(filter, this.caller, event);
    }

    public ListAble getWith(WithFilter filter, CALLER caller, MessageReceivedEvent event) {
        switch (caller){
            case Ships:
                Ship[] results =  Search.shipsWith(filter);
                log.log(Level.INFO, filter.hrInfo());
                if(filter.properties.length > 0) {
                    StringJoiner filtered = new StringJoiner(", ");

                    for (Ship ship : results)
                        filtered.add(ship.name);

                    event.getTextChannel().sendMessage(filtered.toString()).complete();
                } else {
                    event.getTextChannel().sendMessage("Those are not valid arguments").complete();
                }
                break;
            case Auras:
                break;
        } return null;
    }

    @Override
    public String help() {
        switch (caller) {
            case Ships:
                return "\t!ships with VL  –  returns a list with all ships with Vorpal Lance";
            case Auras:
                return "\t!auras with damage > 20";
            default:
                try {
                    throw new OperationNotSupportedException(
                        String.format("No help for caller %s yet", caller.name())
                    );
                } catch (OperationNotSupportedException e) {
                    e.printStackTrace();
                }
        } return null; //Hopefully things never gets here
    }

    private CommandConfig config = new CommandConfig(CommandType.NORMAL, true);

    @Override
    public CommandConfig getConfig() {
        return config;
    }

}
