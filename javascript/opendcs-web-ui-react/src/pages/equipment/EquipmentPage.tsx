import { EquipmentTable } from "./EquipmentTable";
import {
  useDeleteEquipmentMutation,
  useEquipmentRefsQuery,
  useFetchEquipment,
  useSaveEquipmentMutation,
} from "../../queries/equipment";

export const EquipmentPage: React.FC = () => {
  const { data: equipment = [], isLoading } = useEquipmentRefsQuery();
  const fetchEquipment = useFetchEquipment();
  const saveEquipment = useSaveEquipmentMutation();
  const deleteEquipment = useDeleteEquipmentMutation();

  return (
    <div className="content">
      <EquipmentTable
        equipment={equipment}
        loading={isLoading}
        getEquipment={fetchEquipment}
        actions={{
          save: (eq) => saveEquipment.mutateAsync(eq).then(() => {}),
          remove: (equipmentId) => deleteEquipment.mutate(equipmentId),
        }}
      />
    </div>
  );
};
