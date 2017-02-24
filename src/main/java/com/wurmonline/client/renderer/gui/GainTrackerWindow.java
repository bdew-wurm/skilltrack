package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.gui.text.TextFont;
import net.bdew.wurm.skilltrack.GainTracker;
import net.bdew.wurm.skilltrack.SkillTrackMod;
import org.gotti.wurmunlimited.modsupport.console.ConsoleListener;

import java.util.*;
import java.util.stream.Collectors;

public class GainTrackerWindow extends WWindow implements ConsoleListener {
    private final MyTreeList list;
    private final Map<SkillLogic, GainListItem> items;

    public GainTrackerWindow() {
        super("GainTracker");
        setTitle("Skill Gain Tracker");

        width = 450;
        height = 225;

        final int colWidth = TextFont.getText().getWidth("X000.0000X");
        final int colWidth2 = TextFont.getText().getWidth("X000.000000X");
        final int[] colWidths = {colWidth, colWidth, colWidth, colWidth2};
        final String[] colNames = {"Skill", "Gain", "G/Hr", "Avg. Tick"};

        items = new HashMap<>();

        list = new MyTreeList("GainTrackerTree", colWidths, colNames);

        setComponent(list);
    }


    private class MyTreeList extends WurmTreeList<GainListItem> {
        private int sortColumn, sortOrder;

        MyTreeList(String _name, int[] colWidths, String[] colNames) {
            super(_name, colWidths, colNames);
            sortColumn = 1;
            sortOrder = -1;

        }

        @Override
        public void buttonClicked(WButton button) {
            if (button instanceof WurmTreeList.TreeListButton) {
                this.setSort(((WurmTreeList.TreeListButton) button).index);
            }
        }

        void reSort() {
            getNode(null).sortOn(sortColumn, sortOrder);
            recalcLines();
        }

        void setSort(int newSort) {
            if (sortColumn == newSort) {
                sortOrder = -sortOrder;
            } else {
                sortColumn = newSort;
                sortOrder = 1;
            }
            reSort();
        }

        @Override
        protected void rightPressed(int xMouse, int yMouse, int clickCount) {
            GainTrackerWindow.this.rightPressed(xMouse, yMouse, clickCount);
        }
    }

    @Override
    protected void rightPressed(int xMouse, int yMouse, int clickCount) {
        final WurmPopup popup = new WurmPopup("treelistMenu", "Options", xMouse, yMouse);
        popup.addSeparator();
        popup.addButton(popup.new WPopupLiveButton("Reset") {
            protected void handleLeftClick() {
                SkillTrackMod.tracker.skillsCleared();
            }
        });
        hud.showPopupComponent(popup);
    }

    @Override
    public boolean handleInput(String s, Boolean aBoolean) {
        if (s.equals("toggle_gain_tracker")) {
            hud.toggleComponent(this);
            return true;
        }
        return false;
    }

    @Override
    void closePressed() {
        hud.hideComponent(this);
    }

    public void doUpdate(List<GainTracker.Status> gains) {
        Set<SkillLogic> seen = new HashSet<>();
        gains.forEach(e -> {
            if (items.containsKey(e.skill)) {
                items.get(e.skill).update(e);
            } else {
                GainListItem item = new GainListItem(e);
                items.put(e.skill, item);
                list.addTreeListItem(item, null);
            }
            seen.add(e.skill);
        });
        items.entrySet().stream().filter(e -> !seen.contains(e.getKey())).collect(Collectors.toList()).forEach(e -> {
            list.removeTreeListItem(e.getValue());
            items.remove(e.getKey());
        });
        list.reSort();
    }

    class GainListItem extends TreeListItem {
        GainTracker.Status status;
        float lastLevel;

        GainListItem(GainTracker.Status status) {
            this.status = status;
            lastLevel = status.skill.getValue();
        }

        private float fadeFactor = 0.0F;

        void isBeingRenderered() {
            if (this.fadeFactor != 0.0F) {
                this.customRed += this.fadeFactor;
                this.customGreen += this.fadeFactor;
                this.customBlue += this.fadeFactor;
                this.secondaryRed += this.fadeFactor;
                this.secondaryGreen += this.fadeFactor;
                this.secondaryBlue += this.fadeFactor;
                if (this.customRed >= 1.0F && this.customGreen >= 1.0F && this.customBlue >= 1.0F) {
                    this.unsetCustomColor();
                    this.unsetCustomSecondaryColor();
                    this.fadeFactor = 0.0F;
                }
            }

        }

        void update(GainTracker.Status status) {
            this.status = status;
            if (status.skill.getValue() > lastLevel) {
                this.setCustomColor(0.3F, 0.9F, 0.3F);
                this.setCustomSecondaryColor(0.3F, 0.9F, 0.3F);
                this.fadeFactor = 0.005f;
                lastLevel = status.skill.getValue();
            } else if (status.skill.getValue() < lastLevel) {
                this.setCustomColor(0.9F, 0.3F, 0.3F);
                this.setCustomSecondaryColor(0.9F, 0.3F, 0.3F);
                this.fadeFactor = 0.005f;
                lastLevel = status.skill.getValue();
            }
        }

        @Override
        String getName() {
            return status.skill.getName();
        }

        @Override
        void rightClick(int xMouse, int yMouse) {
            GainTrackerWindow.this.rightPressed(xMouse, yMouse, 0);
        }

        @Override
        public void getHoverDescription(PickData pd) {
            super.getHoverDescription(pd);
            pd.addText(String.format("Current level: %.6f", status.skill.getValue()));
            pd.addText(String.format("Gain this session: %.6f", status.skill.getTotalValueChange()));
            if (status.firstTick != status.lastTick)
                pd.addText(String.format("Gain per hour: %.6f", status.gainValue / (status.firstTick - status.lastTick) * 60 * 60 * 1000));
            pd.addText(String.format("Average tick: %.6f", status.avgTick));
        }

        @Override
        int compareTo(TreeListItem o, int i) {
            if (o instanceof GainListItem) {
                GainTracker.Status other = ((GainListItem) o).status;
                switch (i) {
                    case -2:
                        return getName().compareToIgnoreCase(o.getName());
                    case 0:
                        return Float.compare(status.skill.getValue(), other.skill.getValue());
                    case 1:
                        return Float.compare(status.skill.getTotalValueChange(), other.skill.getTotalValueChange());
                    case 2:
                        return Float.compare(
                                (status.firstTick - status.lastTick > 1000) ? status.gainValue / (status.firstTick - status.lastTick) : 0f,
                                (other.firstTick - other.lastTick > 1000) ? other.gainValue / (other.firstTick - other.lastTick) : 0f
                        );
                    case 3:
                        return Float.compare(status.avgTick, other.avgTick);
                    default:
                        return 0;
                }
            } else return 0;
        }

        @Override
        String getParameter(int param) {
            switch (param) {
                case 0:
                    return String.format("%.4f", status.skill.getValue());
                case 1:
                    return String.format("%.4f", status.skill.getTotalValueChange());
                case 2:
                    if (status.firstTick - status.lastTick > 1000)
                        return String.format("%.4f", status.gainValue / (status.firstTick - status.lastTick) * 60 * 60 * 1000);
                    else
                        return "-";
                case 3:
                    return String.format("%.6f", status.avgTick);
                default:
                    return "";
            }

        }
    }
}
