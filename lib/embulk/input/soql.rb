Embulk::JavaPlugin.register_input(
  "soql", "org.embulk.input.soql.SoqlInputPlugin",
  File.expand_path('../../../../classpath', __FILE__))
