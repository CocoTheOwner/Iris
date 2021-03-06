package com.volmit.iris.manager.command.studio;

import com.volmit.iris.Iris;
import com.volmit.iris.IrisSettings;
import com.volmit.iris.generator.noise.CNG;
import com.volmit.iris.manager.IrisDataManager;
import com.volmit.iris.manager.IrisProject;
import com.volmit.iris.object.*;
import com.volmit.iris.util.*;

import java.io.File;
import java.io.IOException;

public class CommandIrisStudioProfile extends MortarCommand {
    public CommandIrisStudioProfile() {
        super("profile", "blame");
        requiresPermission(Iris.perm.studio);
        setDescription("Profile the specified project");
        setCategory("Studio");
    }

    @Override
    public void addTabOptions(MortarSender sender, String[] args, KList<String> list) {

    }

    @Override
    public boolean handle(MortarSender sender, String[] args) {
        if (!IrisSettings.get().isStudio()) {
            sender.sendMessage("To use Iris Studio, please enable studio in Iris/settings.json");
            return true;
        }

        J.a(() -> {
            File f = null;
            File report = Iris.instance.getDataFile("profile.txt");
            KList<String> v = new KList<>();
            if (args.length == 0) {
                if (!Iris.proj.isProjectOpen()) {
                    sender.sendMessage("No open project. Either use /iris std beautify <project> or have a project open.");
                    return;
                }

                f = Iris.proj.getActiveProject().getPath();
            } else {
                f = Iris.instance.getDataFolder("packs", args[0]);

                if (!f.exists()) {
                    sender.sendMessage("Not a valid project.");
                    return;
                }
            }

            IrisProject p = new IrisProject(f);
            IrisDataManager data = new IrisDataManager(f);
            KMap<NoiseStyle, Double> styleTimings = new KMap<>();
            KMap<InterpolationMethod, Double> interpolatorTimings = new KMap<>();
            KMap<String, Double> generatorTimings = new KMap<>();
            KMap<String, Double> biomeTimings = new KMap<>();
            KMap<String, Double> regionTimings = new KMap<>();

            sender.sendMessage("Calculating Performance Metrics for Noise Generators...");


            for (NoiseStyle i : NoiseStyle.values()) {
                CNG c = i.create(new RNG(i.hashCode()));

                for (int j = 0; j < 30000; j++) {
                    c.noise(j, j + 1000, j * j);
                    c.noise(j, -j);
                }

                PrecisionStopwatch px = PrecisionStopwatch.start();

                for (int j = 0; j < 1000000; j++) {
                    c.noise(j, j + 1000, j * j);
                    c.noise(j, -j);
                }

                styleTimings.put(i, px.getMilliseconds());
            }

            v.add("Noise Style Performance Impacts: ");

            for (NoiseStyle i : styleTimings.sortKNumber()) {
                v.add(i.name() + ": " + styleTimings.get(i));
            }

            v.add("");

            sender.sendMessage("Calculating Interpolator Timings...");

            for (InterpolationMethod i : InterpolationMethod.values()) {
                IrisInterpolator in = new IrisInterpolator();
                in.setFunction(i);
                in.setHorizontalScale(8);

                NoiseProvider np = new NoiseProvider() {
                    @Override
                    public double noise(double x, double z) {
                        return Math.random();
                    }
                };

                for (int j = 0; j < 3000; j++) {
                    in.interpolate(j, -j, np);
                }

                PrecisionStopwatch px = PrecisionStopwatch.start();

                for (int j = 0; j < 100000; j++) {
                    in.interpolate(j + 10000, -j - 100000, np);
                }

                interpolatorTimings.put(i, px.getMilliseconds());
            }

            v.add("Noise Interpolator Performance Impacts: ");

            for (InterpolationMethod i : interpolatorTimings.sortKNumber()) {
                v.add(i.name() + ": " + interpolatorTimings.get(i));
            }

            v.add("");

            sender.sendMessage("Processing Generator Scores: ");

            KMap<String, KList<String>> btx = new KMap<>();

            for (String i : data.getGeneratorLoader().getPossibleKeys()) {
                KList<String> vv = new KList<>();
                IrisGenerator g = data.getGeneratorLoader().load(i);
                KList<IrisNoiseGenerator> composites = g.getAllComposites();
                double score = 0;
                int m = 0;
                for (IrisNoiseGenerator j : composites) {
                    m++;
                    score += styleTimings.get(j.getStyle().getStyle());
                    vv.add("Composite Noise Style " + m + " " + j.getStyle().getStyle().name() + ": " + styleTimings.get(j.getStyle().getStyle()));
                }

                score += interpolatorTimings.get(g.getInterpolator().getFunction());
                vv.add("Interpolator " + g.getInterpolator().getFunction().name() + ": " + interpolatorTimings.get(g.getInterpolator().getFunction()));
                generatorTimings.put(i, score);
                btx.put(i, vv);
            }

            v.add("Project Generator Performance Impacts: ");

            for (String i : generatorTimings.sortKNumber()) {
                v.add(i + ": " + generatorTimings.get(i));

                btx.get(i).forEach((ii) -> v.add("  " + ii));
            }

            v.add("");

            KMap<String, KList<String>> bt = new KMap<>();

            for (String i : data.getBiomeLoader().getPossibleKeys()) {
                KList<String> vv = new KList<>();
                IrisBiome b = data.getBiomeLoader().load(i);
                double score = 0;

                int m = 0;
                for (IrisBiomePaletteLayer j : b.getLayers()) {
                    m++;
                    score += styleTimings.get(j.getStyle().getStyle());
                    vv.add("Palette Layer " + m + ": " + styleTimings.get(j.getStyle().getStyle()));
                }

                score += styleTimings.get(b.getBiomeStyle().getStyle());
                vv.add("Biome Style: " + styleTimings.get(b.getBiomeStyle().getStyle()));
                score += styleTimings.get(b.getChildStyle().getStyle());
                vv.add("Child Style: " + styleTimings.get(b.getChildStyle().getStyle()));
                biomeTimings.put(i, score);
                bt.put(i, vv);
            }

            v.add("Project Biome Performance Impacts: ");

            for (String i : biomeTimings.sortKNumber()) {
                v.add(i + ": " + biomeTimings.get(i));

                bt.get(i).forEach((ff) -> v.add("  " + ff));
            }

            v.add("");

            for (String i : data.getRegionLoader().getPossibleKeys()) {
                IrisRegion b = data.getRegionLoader().load(i);
                double score = 0;

                score += styleTimings.get(b.getLakeStyle().getStyle());
                score += styleTimings.get(b.getRiverStyle().getStyle());
                regionTimings.put(i, score);
            }

            v.add("Project Region Performance Impacts: ");

            for (String i : regionTimings.sortKNumber()) {
                v.add(i + ": " + regionTimings.get(i));
            }

            v.add("");

            double m = 0;
            for (double i : biomeTimings.v()) {
                m += i;
            }
            m /= biomeTimings.size();
            double mm = 0;
            for (double i : generatorTimings.v()) {
                mm += i;
            }
            mm /= generatorTimings.size();
            m += mm;
            double mmm = 0;
            for (double i : regionTimings.v()) {
                mmm += i;
            }
            mmm /= regionTimings.size();
            m += mmm;

            v.add("Average Score: " + m);
            sender.sendMessage("Score: " + Form.duration(m, 0));

            try {
                IO.writeAll(report, v.toString("\n"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            sender.sendMessage("Done! " + report.getPath());
        });


        return true;
    }

    @Override
    protected String getArgsUsage() {
        return "[project]";
    }
}
