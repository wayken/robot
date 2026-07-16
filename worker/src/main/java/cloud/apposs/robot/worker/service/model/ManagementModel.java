package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotEmpty;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public class ManagementModel {
    public static class ProviderSync {
        private Table<Param> providers;

        public Table<Param> getProviders() {
            return providers;
        }

        public void setProviders(Table<Param> providers) {
            this.providers = providers;
        }
    }
}
