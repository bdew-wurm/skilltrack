package net.bdew.wurm.skilltrack;

import com.wurmonline.client.game.SkillListener;
import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.game.SkillLogicSet;
import com.wurmonline.client.renderer.gui.GainTrackerWindow;

import java.util.*;
import java.util.stream.Collectors;

public class GainTracker implements SkillListener {
    private class SkillEvent {
        long time;
        float delta;

        public SkillEvent(long time, float delta) {
            this.time = time;
            this.delta = delta;
        }
    }

    public static class Status {
        public final SkillLogic skill;
        public final long firstTick;
        public final long lastTick;
        public final float gainValue;
        public final float avgTick;

        public Status(SkillLogic skill, long firstTick, long lastTick, float gainValue, float avgTick) {
            this.skill = skill;
            this.firstTick = firstTick;
            this.lastTick = lastTick;
            this.gainValue = gainValue;
            this.avgTick = avgTick;
        }
    }

    private GainTrackerWindow window;
    private Map<SkillLogic, Deque<SkillEvent>> skillEvents = new HashMap<>();

    public void register(SkillLogicSet skills) {
        skillsCleared();
        skills.addSkillListener(this);
    }

    public void setWindow(GainTrackerWindow w) {
        this.window = w;
        w.doUpdate(getGains());
    }

    @Override
    public void skillGained(SkillLogic skillLogic) {
        trackChange(skillLogic);
    }

    @Override
    public void skillUpdated(SkillLogic skillLogic) {
        trackChange(skillLogic);
    }

    @Override
    public void skillsCleared() {
        skillEvents.clear();
        if (window != null) window.doUpdate(Collections.emptyList());
    }

    private void trackChange(SkillLogic skill) {
        if (skill.getValueChange()!=0) {
            Deque<SkillEvent> q = skillEvents.get(skill);
            if (q == null) {
                skillEvents.put(skill, q = new LinkedList<>());
            }
            q.addFirst(new SkillEvent(System.currentTimeMillis(), skill.getValueChange()));
            clearOldEvents(5 * 60 * 1000);
            if (window != null) window.doUpdate(getGains());
        }
    }

    private void clearOldEvents(long window) {
        long now = System.currentTimeMillis();
        skillEvents.forEach((s, q) -> q.removeIf(e -> e.time < now - window));
    }

    private List<Status> getGains() {
        long now = System.currentTimeMillis();
        return skillEvents.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> {
                    float gain = e.getValue().stream().map(ev -> ev.delta).reduce(0f, Float::sum);
                    return new Status(
                            e.getKey(),
                            now - e.getValue().peekLast().time,
                            now - e.getValue().peekFirst().time,
                            gain,
                            gain / e.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(o -> (o.lastTick - o.firstTick) / o.gainValue))
                .collect(Collectors.toList());
    }
}
