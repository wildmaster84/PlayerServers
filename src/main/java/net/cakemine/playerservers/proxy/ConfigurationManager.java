package net.cakemine.playerservers.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.common.io.ByteStreams;

import net.cakemine.playerservers.velocity.Utils;

public class ConfigurationManager {

    private final Path dataDirectory;
    public final Yaml yaml;
    private final Utils utils;

    public ConfigurationManager(Path dataDirectory, Utils utils) {
        this.dataDirectory = dataDirectory;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        this.yaml = new Yaml(options);
        this.utils = utils;
    }

    public HashMap<String, ?> loadFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            copyResource(file);
        }
        InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			HashMap<String, Object> data = yaml.load(inputStream);

			return data;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		utils.log(Level.SEVERE, "Failed to load " + fileName);
        return new HashMap<>();
    }
    
    public void loadYamlFile(String fileName, HashMap<String, ?> targetMap) {
        try (InputStream inputStream = new FileInputStream(getDataFolder().getPath() + File.separator + fileName)) {
            targetMap = yaml.load(new InputStreamReader(inputStream, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void createDirectory(String path) {
        File dir = new File(getDataFolder(), path.replace("/", File.separator));
        utils.debug("Directory = " + dir.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void saveConfig(Object config, String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public File getDataFolder() {
    	return dataDirectory.toFile();
    }
    
    public void copyResource(File file) {
        if (!file.exists()) {
            try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(file.getName());
                 FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                ByteStreams.copy(resourceStream, fileOutputStream);
            } catch (IOException e) {
                utils.debug(e.getMessage());
                utils.debug("This is normal for non-config files!");
            }
        }
    }
}

