package com.hack.turbo_collage;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TCShuffle {

    TCShuffle s1;

    TCShuffle s2;

    TCShuffle noUse;

    String uuid;

    double ratio;

    TCJoin tcJoin;


    private double a(double d, double d2) {
        List<TCCollageItemBase> arrayList = new ArrayList<>(b());
        a(arrayList, new TCRect(0.0d, 0.0d, 1.0d, 1.0d));
        Iterator<TCCollageItemBase> it = arrayList.iterator();
        double r0 = 4726483295817170944.0;
        while (true) {
            double c = r0;
            if (!it.hasNext()) {
                return c;
            }
            TCCollageItem fVar = (TCCollageItem) it.next();
            double c2 = Math.min(fVar.ratioRect.right * d, c);
            r0 = Math.min(fVar.ratioRect.bottom * d2, c2);
        }
    }

    // 把所有shuffleList链接起来
    /* renamed from: a */
    private static TCShuffle linkAllShuffles(List<TCShuffle> shuffleList, TCShuffle noUse) {
        TCShuffle result;
        if (shuffleList.size() == 1) {
            result = shuffleList.get(0);
        } else {
            TCShuffle s = new TCShuffle();
            s.tcJoin = TCJoin.TCLeftRightJoin;
            TCShuffle shuffle1 = linkAllShuffles(shuffleList.subList(0, (shuffleList.size() / 2)), s);
            TCShuffle shuffle2 = linkAllShuffles(shuffleList.subList(shuffleList.size() / 2, shuffleList.size()), s);
            s.s1 = shuffle1;
            s.s2 = shuffle2;
            result = s;
        }
        result.noUse = noUse;
        return result;
    }

    /* renamed from: a */
    private static TCShuffle getTotalShuffle(Map<String, Double> ratioMap) {
        TCShuffle shuffle;
        if (ratioMap.size() <= 0) {
            shuffle = null;
        } else {
            Set<String> keySet = ratioMap.keySet();
            List<String> keyList = new ArrayList<>(keySet);
            List<String> randomKeyList = TCUtils.randomList(keyList);
            List<TCShuffle> shuffleList = new ArrayList<>(randomKeyList.size());
            for (String uuid : randomKeyList) {
                Double ratio = ratioMap.get(uuid);
                TCShuffle s = new TCShuffle();
                s.uuid = uuid;
                s.setRatio(ratio == null ? 1.0 : ratio);
                shuffleList.add(s);
            }
            shuffle = linkAllShuffles(shuffleList, null);
        }
        return shuffle;
    }


    public static TCShuffle getTotalShuffle(Map<String, Double> ratioMap, double width, double height) {
        TCShuffle shuffle;
        if (ratioMap.size() <= 0) {
            shuffle = null;
        } else {
            double canvasRatio = width / height;
            TCShuffle a = getTotalShuffle(ratioMap);
            a.b(canvasRatio);
            int i = 0;
            TCShuffle dVar = null;
            double r17 = 0;
            while (i < 500) {
                double z = r17;
                TCShuffle dVar2 = dVar;
                if (Math.abs(a.a() - canvasRatio) < 0.01d) {
                    double z2 = r17;
                    if (dVar == null) {
                        dVar2 = a;
                        z2 = a.a(width, height);
                    }
                    double a2 = a.a(width, height);
                    z = z2;
                    if (a2 > z2) {
                        z = a2;
                        dVar2 = a;
                    }
                }
                TCShuffle a3 = getTotalShuffle(ratioMap);
                a3.b(canvasRatio);
                TCShuffle dVar3 = a;
                if (Math.abs(a3.a() - canvasRatio) < Math.abs(a.a() - canvasRatio)) {
                    dVar3 = a3;
                }
                i++;
                r17 = z;
                dVar = dVar2;
                a = dVar3;
            }
            shuffle = dVar;
            if (dVar == null) {
                shuffle = a;
            }
        }
        return shuffle;
    }

    /* renamed from: a */
    private void setRatio(double setRatio) {
        this.ratio = setRatio;
    }

    private int b() {
        return this.uuid != null ? 1 : this.s1.b() + this.s2.b();
    }


    private void b(double paramDouble) {
        if (this.uuid == null) {
            boolean bool = true;
            label17:
            while (true) {
                if (bool) {
                    Iterator<TCShuffle> it = TCUtils.randomList(c()).iterator();
                    boolean bool1 = false;
                    while (true) {
                        bool = bool1;
                        if (it.hasNext()) {
                            TCShuffle d1 = it.next();
                            double d2 = a();
                            d1.changeJoinType();
                            double d3 = a();
                            if (Math.abs(d3 - paramDouble) < Math.abs(d2 - paramDouble)) {
                                bool1 = true;
                            } else {
                                d1.changeJoinType();
                            }
                            if (Math.abs(d3 - paramDouble) >= 0.01D)
                                continue;
                            return;
                        }
                        continue label17;
                    }
//                    break;
                }
                return;
            }
        }
    }

    private List<TCShuffle> c() {
        List<TCShuffle> list;
        if (this.uuid != null) {
            list = new ArrayList<>();
        } else {
            List<TCShuffle> c = this.s1.c();
            List<TCShuffle> c2 = this.s2.c();
            list = new ArrayList<>(c.size() + c2.size() + 1);
            list.addAll(c);
            list.add(this);
            list.addAll(c2);
        }
        return list;
    }

    /* renamed from: d */
    private void changeJoinType() {
        if (this.tcJoin == TCJoin.TCLeftRightJoin) {
            this.tcJoin = TCJoin.TCUpDownJoin;
        } else {
            this.tcJoin = TCJoin.TCLeftRightJoin;
        }
    }

    public double a() {
        double r10;
        if (this.uuid != null) {
            r10 = this.ratio;
        } else {
            double a = this.s1.a();
            double a2 = this.s2.a();
            r10 = this.tcJoin == TCJoin.TCLeftRightJoin ? a + a2 : 1.0d / ((1.0d / a) + (1.0d / a2));
        }
        return r10;
    }

    public void a(List<TCCollageItemBase> list, TCRect iVar) {
        TCRect iVar2;
        TCRect iVar3;
        if (list != null) {
            if (this.uuid != null) {
                list.add(new TCCollageItem(this.uuid, iVar));
                return;
            }
            double a = this.s1.a();
            double a2 = this.s2.a();
            if (this.tcJoin == TCJoin.TCLeftRightJoin) {
                if (TCUtils.randomBoolean()) {
                    TCRect iVar4 = new TCRect(iVar.left, iVar.top, iVar.right * (a / (a2 + a)), iVar.bottom);
                    iVar2 = new TCRect(iVar.left + iVar4.right, iVar.top, iVar.right - iVar4.right, iVar.bottom);
                    iVar3 = iVar4;
                } else {
                    iVar2 = new TCRect(iVar.left, iVar.top, iVar.right * (a2 / (a + a2)), iVar.bottom);
                    iVar3 = new TCRect(iVar.left + iVar2.right, iVar.top, iVar.right - iVar2.right, iVar.bottom);
                }
            } else if (TCUtils.randomBoolean()) {
                TCRect iVar5 = new TCRect(iVar.left, iVar.top, iVar.right, ((1.0d / a) / ((1.0d / a) + (1.0d / a2))) * iVar.bottom);
                iVar2 = new TCRect(iVar.left, iVar.top + iVar5.bottom, iVar.right, iVar.bottom - iVar5.bottom);
                iVar3 = iVar5;
            } else {
                iVar2 = new TCRect(iVar.left, iVar.top, iVar.right, ((1.0d / a2) / ((1.0d / a) + (1.0d / a2))) * iVar.bottom);
                iVar3 = new TCRect(iVar.left, iVar.top + iVar2.bottom, iVar.right, iVar.bottom - iVar2.bottom);
            }
            this.s1.a(list, iVar3);
            this.s2.a(list, iVar2);
        }
    }
}