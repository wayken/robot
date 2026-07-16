package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.react.React;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.annotation.Action;
import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.robot.worker.WorkerManagement.ManagementProviderSetting;
import cloud.apposs.robot.worker.service.DiskService;
import cloud.apposs.robot.worker.service.ManagementService;
import cloud.apposs.robot.worker.service.WorkerService;
import cloud.apposs.robot.worker.service.model.ManagementModel;
import cloud.apposs.robot.worker.service.model.WorkerModel;
import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.StandardResult;
import cloud.apposs.util.Table;
import cloud.apposs.websocket.WSHttpRequest;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

@Action
public class WorkerHttpApi {
    @Autowired
    private WorkerConfig configuration;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private DiskService diskService;

    @Request.Read("/api/worker/initial")
    public React<StandardResult> initialWorker(@Model WorkerModel.Initialize request) {
        return React.emitter(() -> {
            workerService.initialize(configuration, request);
            return StandardResult.success();
        });
    }

    @Request.Read("/api/worker/status")
    public React<StandardResult> statusWorker() {
        return React.just(StandardResult.success());
    }

    @Request.Read("/api/worker/system")
    public React<StandardResult> systemWorker(@Model WorkerModel.System request) {
        return React.emitter(() -> {
            Param infomation = Param.builder("hostname", handleHostnameResolve())
                    .setString("address", handleLocalAddressResolve())
                    .setString("os", handleOsInfoResolve());
            return StandardResult.success(infomation);
        });
    }

    @Request.Read("/disk/**")
    public React<FileStream> diskFile(WSHttpRequest request) {
        return React.emitter(() -> {
            String requestPath = request.getPath();
            String diskPath = normalizeDiskRequestPath(requestPath);
            int separator = diskPath.indexOf('/');
            if (separator <= 0 || separator + 1 >= diskPath.length()) {
                throw new FileNotFoundException("Disk file not found for path '" + requestPath + "'");
            }
            String wid = diskPath.substring(0, separator);
            String filePath = diskPath.substring(separator + 1);
            FileStream fileStream = diskService.readFileStream(wid, filePath);
            if (fileStream == null) {
                throw new FileNotFoundException("Disk file not found for path '" + requestPath + "'");
            }
            return fileStream.putHeader("Cache-Control", "public, max-age=3600");
        });
    }

    @Request.Post("/api/worker/remove")
    public React<StandardResult> removeWorker(@Model WorkerModel.Remove request) {
        return React.emitter(() -> {
            workerService.removeWorker(configuration, request);
            return StandardResult.success();
        });
    }

    @Request.Post("/api/management/provider/sync")
    public React<StandardResult> managementSyncProvider(@Model ManagementModel.ProviderSync request) {
        return React.emitter(() -> {
            List<Param> providers = request.getProviders() == null ? Table.builder() : request.getProviders();
            Table<ManagementProviderSetting> managementProviders = Table.builder();
            for (Param provider : providers) {
                ManagementProviderSetting setting = new ManagementProviderSetting();
                setting.setName(provider.getString("name"));
                setting.setType(provider.getString("type"));
                setting.setUrl(provider.getString("url"));
                Table<Param> models = provider.getTable("models");
                setting.setModels(models != null ? models : new ArrayList<>());
                Table<String> keys = provider.getTable("api_keys");
                setting.setKeys(keys != null ? keys : new ArrayList<>());
                managementProviders.add(setting);
            }
            managementService.syncProvider(configuration, managementProviders);
            return StandardResult.success();
        });
    }

    private String handleHostnameResolve() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String handleLocalAddressResolve() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String handleOsInfoResolve() {
        return System.getProperty("os.name", "unknown")
                + " " + System.getProperty("os.version", "")
                + " " + System.getProperty("os.arch", "");
    }

    private String normalizeDiskRequestPath(String requestPath) throws Exception {
        if (requestPath == null || !requestPath.startsWith("/disk/")) {
            return "";
        }
        String path = requestPath.substring("/disk/".length());
        if (path.indexOf('%') >= 0) {
            path = URLDecoder.decode(path, CharsetUtil.UTF_8.name());
        }
        path = path.replace('\\', '/');
        String[] segments = path.split("/");
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            if (segment.length() == 0 || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("Invalid disk path '" + requestPath + "'");
            }
            if (result.length() > 0) {
                result.append('/');
            }
            result.append(segment);
        }
        return result.toString();
    }
}
