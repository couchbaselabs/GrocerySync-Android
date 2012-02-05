{application, couch, [
    {description, "Apache CouchDB"},
    {vsn, "1.2.0a-573eba5-git"},
    {modules, [couch_native_process,couch_os_daemons,couch_httpd_rewrite,couch,couch_httpd_stats_handlers,couch_ejson_compare,couch_httpd_oauth,json_stream_parse,couch_primary_sup,couch_httpd_proxy,couch_httpd_vhost,couch_changes,couch_compress,couch_httpd_view_merger,couch_api_wrap,couch_view_compactor,couch_config,couch_stats_collector,couch_httpd_view,couch_httpd_external,couch_server_sup,couch_drv,couch_replication_notifier,couch_view_updater,couch_uuids,couch_auth_cache,couch_secondary_sup,couch_httpd_misc_handlers,couch_util,couch_config_writer,couch_httpc_pool,couch_replicator_utils,couch_api_wrap_httpc,couch_app,couch_access_log,couch_external_manager,jninif,couch_log,couch_db,couch_os_process,couch_query_servers,couch_btree,couch_work_queue,couch_httpd,couch_task_status,couch_skew,couch_db_update_notifier_sup,couch_replication_manager,couch_db_updater,couch_compaction_daemon,couch_server,couch_rep_sup,couch_httpd_db,couch_view_group,couch_compress_types,couch_view_merger_queue,couch_key_tree,couch_view_merger,couch_replicator,couch_db_frontend,couch_internal_load_gen,couch_replicator_worker,couch_db_update_notifier,couch_stream,couch_httpd_auth,couch_httpd_show,couch_indexer_manager,couch_doc,couch_view,couch_btree_copy,couch_event_sup,couch_file,couch_httpd_replicator,couch_ref_counter,couch_external_server,couch_stats_aggregator]},
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
