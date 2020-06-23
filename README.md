# Spigot-UpdateChecker
Integrate automatic update checks into your own plugin!

## Import via maven
First you need to add the UpdateChecker as a dependency to your pom.xml:

```
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
	    <version>1.0</version>
    </dependency>
</dependencies>
```

It is also important to use the maven-shade-plugin as a plugin inside your <plugins> section. Please do not forget to insert your own plugin's package name inside <shadedPattern>.

```
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
Now you can create an instance of the PluginUpdateChecker class, e.g. in your onEnable method:

```
import de.jeff_media.PluginUpdateChecker.PluginUpdateChecker;

...

public class MyPlugin extends JavaPlugin implements Listener {

    PluginUpdateChecker updateChecker;

    public void onEnable() {

        // Link to a text file containing the latest version.
        // You can use the Spigot API, just replace the resource id.
        String version = "https://api.spigotmc.org/legacy/update.php?resource=59773";
        String downloadLink = "https://www.chestsort.de"
        String changelogLink = "https://www.chestsort.de/changelog"
        String donateLink = "https://chestsort.de/donate"

        updateChecker = new PluginUpdateChecker(this,version,downloadLink,changelogLink,donateLink,);

        // Check every hour for updates:
        updateChecker.check(3600);

        // ... or check only once:
        updateChecker.check();

        ...
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
    
        // Send a message to OPs joining the server, if a new version has been found
        updateChecker.sendUpdateMessage(e.getPlayer());

        ...

    }
}
```