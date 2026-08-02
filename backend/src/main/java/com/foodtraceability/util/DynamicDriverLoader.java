package com.foodtraceability.util;

import com.foodtraceability.driver.DeviceDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 动态驱动加载器
 * 负责从外部目录加载驱动jar包，并实例化驱动对象
 */
@Component
public class DynamicDriverLoader {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicDriverLoader.class);
    
    // 驱动目录
    private static final String DRIVER_DIRECTORY = "drivers";
    
    // 驱动类加载器列表
    private final List<URLClassLoader> driverClassLoaders = new ArrayList<>();
    
    /**
     * 加载指定目录下的所有驱动jar包
     * @return 加载的驱动实例列表
     */
    public List<DeviceDriver> loadDriversFromDirectory() {
        return loadDriversFromDirectory(DRIVER_DIRECTORY);
    }
    
    /**
     * 加载指定目录下的所有驱动jar包
     * @param directoryPath 驱动目录路径
     * @return 加载的驱动实例列表
     */
    public List<DeviceDriver> loadDriversFromDirectory(String directoryPath) {
        List<DeviceDriver> loadedDrivers = new ArrayList<>();
        
        File driverDir = new File(directoryPath);
        if (!driverDir.exists()) {
            log.warn("驱动目录 {} 不存在，创建目录", directoryPath);
            driverDir.mkdirs();
            return loadedDrivers;
        }
        
        if (!driverDir.isDirectory()) {
            log.error("{} 不是一个目录", directoryPath);
            return loadedDrivers;
        }
        
        // 获取目录下的所有jar文件
        File[] jarFiles = driverDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("驱动目录 {} 下没有jar文件", directoryPath);
            return loadedDrivers;
        }
        
        log.info("发现 {} 个驱动jar文件，开始加载", jarFiles.length);
        
        // 加载每个jar文件
        for (File jarFile : jarFiles) {
            try {
                List<DeviceDriver> drivers = loadDriversFromJar(jarFile);
                loadedDrivers.addAll(drivers);
            } catch (Exception e) {
                log.error("加载驱动jar包 {} 失败: {}", jarFile.getName(), e.getMessage(), e);
            }
        }
        
        log.info("成功加载 {} 个驱动", loadedDrivers.size());
        return loadedDrivers;
    }
    
    /**
     * 从指定的jar文件加载驱动
     * @param jarFile jar文件
     * @return 加载的驱动实例列表
     * @throws Exception 加载异常
     */
    public List<DeviceDriver> loadDriversFromJar(File jarFile) throws Exception {
        List<DeviceDriver> drivers = new ArrayList<>();
        
        log.info("开始加载驱动jar包: {}", jarFile.getName());
        
        // 创建jar文件对象
        try (JarFile jar = new JarFile(jarFile)) {
            // 获取jar文件中的所有条目
            List<String> driverClassNames = new ArrayList<>();
            
            jar.stream()
               .filter(entry -> entry.getName().endsWith(".class"))
               .forEach(entry -> {
                   // 转换为类名
                   String className = entry.getName().replace(".class", "").replace("/", ".");
                   driverClassNames.add(className);
               });
            
            // 创建URL数组
            URL[] urls = new URL[]{jarFile.toURI().toURL()};
            
            // 创建类加载器
            URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            driverClassLoaders.add(classLoader);
            
            // 加载每个类，检查是否是DeviceDriver的实现类
            for (String className : driverClassNames) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    
                    // 检查是否实现了DeviceDriver接口
                    if (DeviceDriver.class.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isEnum()) {
                        log.info("发现驱动类: {}", className);
                        
                        // 实例化驱动对象
                        DeviceDriver driver = (DeviceDriver) clazz.getDeclaredConstructor().newInstance();
                        drivers.add(driver);
                        log.info("成功实例化驱动: {} 名称: {} 版本: {}", 
                                className, driver.getDriverName(), driver.getDriverVersion());
                    }
                } catch (Exception e) {
                    // 忽略无法加载的类
                    log.debug("无法加载类 {}: {}", className, e.getMessage());
                }
            }
        }
        
        log.info("成功从jar包 {} 加载 {} 个驱动", jarFile.getName(), drivers.size());
        return drivers;
    }
    
    /**
     * 加载单个驱动文件
     * @param jarFilePath 驱动jar文件路径
     * @return 加载的驱动实例列表
     * @throws Exception 加载异常
     */
    public List<DeviceDriver> loadDriver(String jarFilePath) throws Exception {
        File jarFile = new File(jarFilePath);
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("驱动文件不存在: " + jarFilePath);
        }
        
        return loadDriversFromJar(jarFile);
    }
    
    /**
     * 卸载所有加载的驱动
     */
    public void unloadAllDrivers() {
        log.info("开始卸载所有动态加载的驱动");
        
        // 关闭所有类加载器
        for (URLClassLoader classLoader : driverClassLoaders) {
            try {
                classLoader.close();
            } catch (Exception e) {
                log.error("关闭驱动类加载器失败: {}", e.getMessage(), e);
            }
        }
        
        driverClassLoaders.clear();
        log.info("所有动态加载的驱动已卸载");
    }
    
    /**
     * 获取驱动目录
     * @return 驱动目录
     */
    public String getDriverDirectory() {
        return DRIVER_DIRECTORY;
    }
}