# Spigot-UpdateChecker
Integrate automatic update checks into your own plugin! Automatically prints a console message when an update has been found and sends messages to server operators including a download link and more, if you wish.

## Import via maven
First you need to add the UpdateChecker as a dependency to your pom.xml:

```xml
<repositories>
    <repository>
	    <id>jeff-media-gbr</id>
	    <url>https://repo.jeff-media.de/maven2</url>
    </repository>
</repositories>		

<dependencies>
    <dependency>
        <groupId>de.jeff_media</groupId>
	    <artifactId>PluginUpdateChecker</artifactId>
	    <version>[1.3,)</version> <!-- This should always get you the latest version -->
            <scope>compile</scope>
    </dependency>
</dependencies>
```

It is also important to use the maven-shade-plugin as a plugin inside your `<plugins>` section. Please do not forget to insert your own plugin's package name inside `<shadedPattern>`.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.1.0</version>
    <configuration>
      <relocations>
        <relocation>
          <pattern>de.jeff_media.PluginUpdateChecker</pattern>
          <shadedPattern>[Your plugin's package name]</shadedPattern>
        </relocation>
      </relocations>
    </configuration>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

You can view an example of a pom.xml containing the above content [HERE](https://github.com/JEFF-Media-GbR/Spigot-UpdateChecker/blob/master/example-pom.xml).

## Usage
Now you can create an instance of the PluginUpdateChecker class, e.g. in your onEnable method. If a new version has been found, a message will be printed in the console. Every server operator will also be informed when they join the server.

#### Example 1: Async check once every hour
```java
import de.jeff_media.PluginUpdateChecker.PluginUpdateChecker;

public class MyPlugin extends JavaPlugin {

    PluginUpdateChecker updateChecker;

    public void onEnable() {

        // Link to a text file containing the latest version.
        // You can use the Spigot API, just replace the resource id.
        String version = "https://api.spigotmc.org/legacy/update.php?resource=59773";
        String downloadLink = "https://www.chestsort.de";
        String changelogLink = null;
        String donateLink = "https://chestsort.de/donate";

        updateChecker = new PluginUpdateChecker(this,version,downloadLink,changelogLink,donateLink,);

        // Check every hour for updates (async):
        updateChecker.check(3600);

        // ... or only once (async):
        updateChecker.check();

        // You can also force a synced check to get a result immediately,
        // however this is not recommended as it blocks the main thread
        // until the HTTP(S) requests completes or timeouts.
        boolean newVersionAvailable = updateChecker.forceSyncedCheck();
        if(newVersionAvailable) {
            getLogger().severe("You must update this plugin to use it!");
            getServer().getPluginManager().disablePlugin(this);
        } 

        ...
    }
}
```
