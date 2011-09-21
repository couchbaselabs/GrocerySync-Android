{application, couch, [
    {description, "Apache CouchDB"},
    {vsn, "1.2.0a-361a925-git"},
    {modules, [couch_db_update_notifier,couch_util,couch_skew,couch_task_status,couch_primary_sup,couch_stats_collector,couch_external_server,couch_config_writer,couch_log,couch_native_process,couch_httpd_view_merger,couch_replication_manager,couch_config,couch_event_sup,couch_file,couch_httpd_replicator,couch_replicator_worker,couch_stream,couch_db_updater,couch_view_updater,couch_httpd_show,couch_compress,couch_replicator,couch_internal_load_gen,couch_key_tree,couch_stats_aggregator,couch_httpc_pool,couch_doc,couch_os_process,couch_httpd_rewrite,couch_drv,couch_db,couch_api_wrap_httpc,couch_secondary_sup,couch_server,couch_changes,couch_work_queue,couch_os_daemons,couch_httpd_external,couch_view_merger,couch_ref_counter,couch_indexer_manager,couch,couch_view_merger_queue,couch_compaction_daemon,couch_btree,couch_httpd_auth,couch_rep_sup,couch_app,couch_httpd_db,couch_external_manager,couch_access_log,couch_uuids,couch_view_compactor,couch_replication_notifier,couch_query_servers,couch_view_group,couch_httpd_vhost,couch_httpd_oauth,couch_httpd_misc_handlers,couch_replicator_utils,couch_httpd_proxy,couch_httpd_stats_handlers,couch_auth_cache,couch_server_sup,couch_compress_types,jninif,couch_db_frontend,couch_httpd_view,couch_httpd,couch_api_wrap,json_stream_parse,couch_ejson_compare,couch_db_update_notifier_sup,couch_view]},
    {registered, [
        couch_config,
        couch_db_update,
        couch_db_update_notifier_sup,
        couch_external_manager,
        couch_httpd,
        couch_log,
        couch_access_log,
        couch_primary_services,
        couch_query_servers,
        couch_rep_sup,
        couch_secondary_services,
        couch_server,
        couch_server_sup,
        couch_stats_aggregator,
        couch_stats_collector,
        couch_task_status,
        couch_view
    ]},
    {mod, {couch_app, [
        "/data/data/%app_name%/couchdb/etc/couchdb/default.ini",
        "/data/data/%app_name%/couchdb/etc/couchdb/local.ini"
    ]}},
    {applications, [kernel, stdlib]},
    {included_applications, [crypto, sasl, ibrowse, mochiweb, os_mon]}
]}.
