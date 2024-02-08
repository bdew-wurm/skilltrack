package net.bdew.wurm.skilltrack;

import com.wurmonline.client.renderer.gui.GainTrackerWindow;
import com.wurmonline.client.renderer.gui.MainMenu;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.settings.SavePosManager;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.console.ModConsole;

import java.lang.reflect.Method;
import java.util.List;

public class EarlyLoadingFixer {

    public static Object headsUpDisplayHook(Object proxy, Method method, Object[] args) {
        try {
            method.invoke(proxy, args);

            GainTrackerWindow trackerWindow = new GainTrackerWindow();
            SkillTrackMod.tracker.setWindow(trackerWindow);

            MainMenu mainMenu = ReflectionUtil.getPrivateField(proxy, ReflectionUtil.getField(proxy.getClass(), "mainMenu"));
            List<WurmComponent> components = ReflectionUtil.getPrivateField(proxy, ReflectionUtil.getField(proxy.getClass(), "components"));
            SavePosManager savePosManager = ReflectionUtil.getPrivateField(proxy, ReflectionUtil.getField(proxy.getClass(), "savePosManager"));

            components.add(trackerWindow);
            mainMenu.registerComponent("Gain Tracker", trackerWindow);
            savePosManager.registerAndRefresh(trackerWindow, "gaintracker");
            ModConsole.addConsoleListener(trackerWindow);

            SkillTrackMod.logInfo("Window registered");
        } catch (Throwable e) {
            SkillTrackMod.logException("Error loading mod", e);
        }
        return null;
    }
}
