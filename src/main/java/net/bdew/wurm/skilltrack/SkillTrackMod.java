package net.bdew.wurm.skilltrack;

import com.wurmonline.client.game.SkillLogicSet;
import com.wurmonline.client.renderer.gui.GainTrackerWindow;
import com.wurmonline.client.renderer.gui.MainMenu;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.settings.SavePosManager;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;
import org.gotti.wurmunlimited.modsupport.console.ModConsole;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkillTrackMod implements WurmClientMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("SkillTrackMod");

    public static GainTracker tracker;

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }


    @Override
    public void init() {
        try {
            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);

                GainTrackerWindow trackerWindow = new GainTrackerWindow();
                tracker.setWindow(trackerWindow);

                MainMenu mainMenu = ReflectionUtil.getPrivateField(proxy, ReflectionUtil.getField(proxy.getClass(), "mainMenu"));
                List<WurmComponent> components = ReflectionUtil.getPrivateField(proxy, ReflectionUtil.getField(proxy.getClass(), "components"));
                SavePosManager savePosManager = ReflectionUtil.getPrivateField(proxy, ReflectionUtil.getField(proxy.getClass(), "savePosManager"));

                components.add(trackerWindow);
                mainMenu.registerComponent("Gain Tracker", trackerWindow);
                savePosManager.registerAndRefresh(trackerWindow, "gaintracker");
                ModConsole.addConsoleListener(trackerWindow);

                logInfo("Window registered");

                return null;
            });

            HookManager.getInstance().registerHook("com.wurmonline.client.settings.Profile$PlayerProfile", "registerSkillSet", "(Lcom/wurmonline/client/game/SkillLogicSet;)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);

                tracker = new GainTracker();
                tracker.register((SkillLogicSet) args[0]);

                logInfo("Tracker registered");

                return null;
            });
        } catch (Throwable e) {
            logException("Error loading mod", e);
        }
    }

    @Override
    public void preInit() {

    }
}
