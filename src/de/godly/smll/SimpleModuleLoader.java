package de.godly.smll;

import de.godly.smll.util.ReflectionUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleModuleLoader {

    public List<Module> loadedModules = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger("SimpleModuleLoader");
    public static SimpleModuleLoader INSTANCE;
    @Getter
    public URLClassLoader mainClassLoader;


    public boolean running = true;
    public File modulesFile = new File(".", "modules");

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public SimpleModuleLoader() {
        INSTANCE = this;
        try {
            if (!modulesFile.exists()) modulesFile.mkdirs();
            List<URL> urls = new ArrayList<>();
            //TODO: Get all possible Module jar files in subfolder modules
            Arrays.stream(modulesFile.listFiles()).filter(file -> file.getName().endsWith(".jar")).forEach(file -> {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            });
            //TODO: Add this jar itself so we can load ModulePrio1.class & ModulePrio5.class
            urls.add(SimpleModuleLoader.this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().toURL());
            //TODO: Instantiate our URLClassLoader with the URLs cause they cant be changed later cause "security threat"
            mainClassLoader = new URLClassLoader(urls.toArray(new URL[0]), SimpleModuleLoader.class.getClassLoader());
            ModuleLoader loader = new ModuleLoader();
            List<Class<? extends Module>> toLoad = new ArrayList<>();
            urls.forEach(url -> {
                try {
                    toLoad.addAll(loader.load(url.getPath(), null, null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            toLoad.forEach(clsToLoad -> {
                try {
                    //TODO: Instantiate Module Classes
                    Constructor<? extends Module> moduleConst = clsToLoad.getConstructor();
                    Module moduleInstance = moduleConst.newInstance();
                    loadedModules.add(moduleInstance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            loadedModules.forEach(mod -> {
                getLogger().log(Level.INFO, "Loading Module " + mod.getName() + " version " + mod.getVersion());
                mod.onLoad();

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void main(String[] args) {
        try {
            new SimpleModuleLoader();
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (INSTANCE.running) {
        }
        INSTANCE.loadedModules.forEach(Module::onUnload);
    }
}
