package process.context;

import context.AbstractApplicationContext;

public class ApplicationContext extends AbstractApplicationContext<ApplicationParameters, ApplicationEvents> {

    public ApplicationContext(String configFileName) {
        super(configFileName);
    }
}
